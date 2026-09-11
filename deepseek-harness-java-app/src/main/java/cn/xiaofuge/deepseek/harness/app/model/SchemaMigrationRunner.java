package cn.xiaofuge.deepseek.harness.app.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

/**
 * 启动时幂等执行增量列迁移。
 * <p>MySQL 8.0 不支持 {@code ALTER TABLE ... ADD COLUMN IF NOT EXISTS}，
 * 因此先查 information_schema 判断列是否存在，再决定是否执行 ALTER；H2 亦兼容该写法。
 * <p>优先级设为最高，保证在 {@link ModelSyncRunner} 等读写模型配置的 Runner 之前执行。
 * <p>示例：存量库缺少 harness_model_setting.protocol 时启动自动补列，默认 openai。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrationRunner.class);

    private final DataSource dataSource;

    /**
     * 初始化迁移执行器。
     *
     * @param dataSource 应用数据源。
     */
    public SchemaMigrationRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    /**
     * 启动后执行增量迁移；失败仅记录日志，不阻断启动。
     *
     * @param args Spring Boot 启动参数。
     */
    public void run(ApplicationArguments args) {
        try {
            addColumnIfMissing(
                    "harness_model_setting", "protocol",
                    "ALTER TABLE harness_model_setting ADD COLUMN protocol VARCHAR(32) NOT NULL DEFAULT 'openai'");
            addColumnIfMissing(
                    "harness_model_setting", "active",
                    "ALTER TABLE harness_model_setting ADD COLUMN active BOOLEAN NOT NULL DEFAULT FALSE");
            migrateToCompositePrimaryKey();
            backfillActiveForSingleModelProfiles();
        } catch (Exception e) {
            log.error("Schema migration failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 存量库主键为 profile_code 单列时，迁移为 (profile_code, model_code) 联合主键。
     * <p>流程：经 DatabaseMetaData 读主键列 -> 已是联合主键则跳过 -> 否则重建主键。
     * MySQL 与 H2 均兼容 DROP/ADD PRIMARY KEY 写法。
     */
    private void migrateToCompositePrimaryKey() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            if (primaryKeyColumnCount(conn) >= 2) {
                return;
            }
            try (var st = conn.createStatement()) {
                st.execute("ALTER TABLE harness_model_setting DROP PRIMARY KEY");
                st.execute("ALTER TABLE harness_model_setting ADD PRIMARY KEY (profile_code, model_code)");
                log.info("Schema migration: harness_model_setting primary key -> (profile_code, model_code)");
            }
        }
    }

    /**
     * 读取 harness_model_setting 的主键列数；表不存在时返回 0。
     */
    private int primaryKeyColumnCount(Connection conn) throws Exception {
        int count = 0;
        try (ResultSet rs = conn.getMetaData().getPrimaryKeys(conn.getCatalog(), null, "harness_model_setting")) {
            while (rs.next()) {
                count++;
            }
        }
        if (count == 0) {
            // H2 可能返回大写表名，兜底再查一次
            try (ResultSet rs = conn.getMetaData().getPrimaryKeys(conn.getCatalog(), null, "HARNESS_MODEL_SETTING")) {
                while (rs.next()) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 存量数据回填：每个 profile 仅一条配置且未激活时，将其置为当前生效模型。
     * <p>MySQL 不允许在 UPDATE 的子查询中直接引用被更新的表，
     * 因此子查询外包一层派生表规避该限制（H2 亦兼容）。
     */
    private void backfillActiveForSingleModelProfiles() throws Exception {
        try (Connection conn = dataSource.getConnection();
             var st = conn.createStatement()) {
            int updated = st.executeUpdate(
                    "UPDATE harness_model_setting t SET t.active = TRUE "
                            + "WHERE t.active = FALSE AND t.profile_code IN ("
                            + "SELECT s.profile_code FROM ("
                            + "SELECT x.profile_code FROM harness_model_setting x "
                            + "GROUP BY x.profile_code HAVING COUNT(*) = 1) s)");
            if (updated > 0) {
                log.info("Schema migration: backfilled active for {} single-model profile(s)", updated);
            }
        }
    }

    /**
     * 判断列是否存在，缺失时执行 ALTER。
     * <p>流程：经 DatabaseMetaData 读列元数据（大小写各查一次，兼容 MySQL/H2）-> 存在则跳过 -> 否则执行迁移语句并记录日志。
     *
     * @param table 表名。
     * @param column 列名。
     * @param ddl 列缺失时执行的 DDL。
     */
    private void addColumnIfMissing(String table, String column, String ddl) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            if (columnExists(conn, table, column)) {
                return;
            }
            try (var st = conn.createStatement()) {
                st.execute(ddl);
                log.info("Schema migration: added column {}.{}", table, column);
            }
        }
    }

    /**
     * 经 DatabaseMetaData 判断列是否存在；MySQL 用小写标识符、H2 用大写，各查一次。
     */
    private boolean columnExists(Connection conn, String table, String column) throws Exception {
        String[][] candidates = {
                {table, column},
                {table.toUpperCase(), column.toUpperCase()},
                {table.toLowerCase(), column.toLowerCase()}
        };
        for (String[] pair : candidates) {
            try (ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, pair[0], pair[1])) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        return false;
    }
}
