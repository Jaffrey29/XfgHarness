package cn.xiaofuge.deepseek.harness.infrastructure.dao;

import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.DigitalHumanPO;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 数字人身份持久化。
 */
@Mapper
public interface IDigitalHumanDao {

    @Insert("""
            INSERT INTO digital_human (id, display_name, avatar_ref, purpose, role_tags, theme_color, approval_policy, concurrency_limit, created_at, updated_at)
            VALUES (#{id}, #{displayName}, #{avatarRef}, #{purpose}, #{roleTags}, #{themeColor}, #{approvalPolicy}, #{concurrencyLimit}, #{createdAt}, #{updatedAt})
            """)
    void insert(DigitalHumanPO po);

    @Update("""
            UPDATE digital_human
            SET display_name = #{displayName}, avatar_ref = #{avatarRef}, purpose = #{purpose},
                role_tags = #{roleTags}, theme_color = #{themeColor}, approval_policy = #{approvalPolicy},
                concurrency_limit = #{concurrencyLimit}, updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int update(DigitalHumanPO po);

    @Select("""
            SELECT id, display_name, avatar_ref, purpose, role_tags, theme_color, approval_policy, concurrency_limit, created_at, updated_at
            FROM digital_human ORDER BY created_at ASC
            """)
    List<DigitalHumanPO> listAll();

    @Select("""
            SELECT id, display_name, avatar_ref, purpose, role_tags, theme_color, approval_policy, concurrency_limit, created_at, updated_at
            FROM digital_human WHERE id = #{id}
            """)
    Optional<DigitalHumanPO> findById(@Param("id") String id);

    @Delete("DELETE FROM digital_human WHERE id = #{id}")
    int deleteById(@Param("id") String id);
}
