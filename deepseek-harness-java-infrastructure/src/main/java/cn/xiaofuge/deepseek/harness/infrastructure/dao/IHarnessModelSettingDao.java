package cn.xiaofuge.deepseek.harness.infrastructure.dao;

import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.HarnessModelSettingPO;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 模型设置 DAO 接口。
 * <p>
 * 执行流程：
 * 1. 仓储层调用 DAO 访问数据库；
 * 2. DAO 负责 PO 与表结构映射；
 * 3. 返回数据库记录或完成写入。
 *
 * <p>调用流程：领域端口 → 基础设施适配器 → 外部系统。</p>
 * <p>说明：表主键为 (profile_code, model_code)，同一 profile 可挂多个模型；
 * {@code active} 标记该 profile 当前生效的模型，运行时按 profile 查询时优先取激活行。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Mapper
public interface IHarnessModelSettingDao {

    @Insert("""
            INSERT INTO harness_model_setting (profile_code, provider_code, model_code, base_url, api_key_ref, protocol, enabled, active, updated_at)
            VALUES (#{profileCode}, #{providerCode}, #{modelCode}, #{baseUrl}, #{apiKeyRef}, #{protocol}, #{enabled}, #{active}, #{updatedAt})
            ON DUPLICATE KEY UPDATE
                provider_code = VALUES(provider_code),
                base_url = VALUES(base_url),
                api_key_ref = VALUES(api_key_ref),
                protocol = VALUES(protocol),
                enabled = VALUES(enabled),
                active = VALUES(active),
                updated_at = VALUES(updated_at)
            """)
    void save(HarnessModelSettingPO modelSettingPO);

    @Select("""
            SELECT profile_code, provider_code, model_code, base_url, api_key_ref, protocol, enabled, active, updated_at
            FROM harness_model_setting
            ORDER BY profile_code ASC, active DESC, updated_at ASC
            """)
    List<HarnessModelSettingPO> queryAll();

    /**
     * 查询某 profile 当前生效的模型设置；无激活行时退化为该 profile 最新一条。
     */
    @Select("""
            SELECT profile_code, provider_code, model_code, base_url, api_key_ref, protocol, enabled, active, updated_at
            FROM harness_model_setting
            WHERE profile_code = #{profileCode}
            ORDER BY active DESC, updated_at ASC
            LIMIT 1
            """)
    Optional<HarnessModelSettingPO> findByProfileCode(@Param("profileCode") String profileCode);

    @Select("""
            SELECT profile_code, provider_code, model_code, base_url, api_key_ref, protocol, enabled, active, updated_at
            FROM harness_model_setting
            WHERE profile_code = #{profileCode} AND model_code = #{modelCode}
            """)
    Optional<HarnessModelSettingPO> findByProfileAndModel(@Param("profileCode") String profileCode,
                                                          @Param("modelCode") String modelCode);

    /**
     * 将指定模型置为该 profile 当前生效项；同 profile 其他行先置为非激活。
     */
    @Update("""
            UPDATE harness_model_setting SET active = FALSE, updated_at = updated_at
            WHERE profile_code = #{profileCode}
            """)
    void clearActiveByProfile(@Param("profileCode") String profileCode);

    @Update("""
            UPDATE harness_model_setting SET active = TRUE, enabled = TRUE, updated_at = #{updatedAt}
            WHERE profile_code = #{profileCode} AND model_code = #{modelCode}
            """)
    int activate(@Param("profileCode") String profileCode,
                 @Param("modelCode") String modelCode,
                 @Param("updatedAt") java.time.Instant updatedAt);

    /**
     * 删除某 profile 下的指定模型。
     */
    @Delete("""
            DELETE FROM harness_model_setting WHERE profile_code = #{profileCode} AND model_code = #{modelCode}
            """)
    void deleteByProfileAndModel(@Param("profileCode") String profileCode,
                                 @Param("modelCode") String modelCode);

    /**
     * 删除某 profile 下全部模型配置。
     */
    @Delete("""
            DELETE FROM harness_model_setting WHERE profile_code = #{profileCode}
            """)
    void deleteByProfileCode(@Param("profileCode") String profileCode);
}
