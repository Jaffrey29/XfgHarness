package cn.xiaofuge.deepseek.harness.infrastructure.dao;

import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.HarnessModelSettingPO;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

/**
 * 模型设置 DAO 接口。
 * <p>
 * 执行流程：
 * 1. 仓储层调用 DAO 访问数据库；
 * 2. DAO 负责 PO 与表结构映射；
 * 3. 返回数据库记录或完成写入。
 *
 * <p>调用流程：领域端口 → 基础设施适配器 → 外部系统。</p>
 * <p>示例：适配器只实现端口契约，业务规则保留在 domain。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Mapper
public interface IHarnessModelSettingDao {

    @Insert("""
            INSERT INTO harness_model_setting (profile_code, provider_code, model_code, base_url, api_key_ref, enabled, updated_at)
            VALUES (#{profileCode}, #{providerCode}, #{modelCode}, #{baseUrl}, #{apiKeyRef}, #{enabled}, #{updatedAt})
            ON DUPLICATE KEY UPDATE
                provider_code = VALUES(provider_code),
                model_code = VALUES(model_code),
                base_url = VALUES(base_url),
                api_key_ref = VALUES(api_key_ref),
                enabled = VALUES(enabled),
                updated_at = VALUES(updated_at)
            """)
    void save(HarnessModelSettingPO modelSettingPO);

    @Select("""
            SELECT profile_code, provider_code, model_code, base_url, api_key_ref, enabled, updated_at
            FROM harness_model_setting
            ORDER BY profile_code ASC
            """)
    List<HarnessModelSettingPO> queryAll();

    @Select("""
            SELECT profile_code, provider_code, model_code, base_url, api_key_ref, enabled, updated_at
            FROM harness_model_setting
            WHERE profile_code = #{profileCode}
            """)
    Optional<HarnessModelSettingPO> findByProfileCode(@Param("profileCode") String profileCode);

    @Delete("""
            DELETE FROM harness_model_setting WHERE profile_code = #{profileCode}
            """)
    void deleteByProfileCode(@Param("profileCode") String profileCode);
}
