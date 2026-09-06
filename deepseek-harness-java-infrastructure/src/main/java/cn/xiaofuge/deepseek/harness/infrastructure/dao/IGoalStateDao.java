package cn.xiaofuge.deepseek.harness.infrastructure.dao;

import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.GoalStatePO;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 目标状态 DAO 接口。
 * <p>
 * 执行流程：
 * 1. 仓储层组装目标快照；
 * 2. DAO 负责映射表结构；
 * 3. 返回持久化结果。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Mapper
public interface IGoalStateDao {

    /**
     * 保存或更新目标快照。
     * <p>
     * 执行流程：
     * 1. 接收目标 PO；
     * 2. 按 session_id 做幂等写入；
     * 3. 更新为最新状态。
     */
    @Insert("""
            INSERT INTO harness_goal_state (
                session_id, goal_id, revision, title, description, status,
                blocked_reason, max_rounds, rounds_started, continuation_enabled,
                created_at, updated_at
            ) VALUES (
                #{sessionId}, #{goalId}, #{revision}, #{title}, #{description}, #{status},
                #{blockedReason}, #{maxRounds}, #{roundsStarted}, #{continuationEnabled},
                #{createdAt}, #{updatedAt}
            )
            ON DUPLICATE KEY UPDATE
                goal_id = VALUES(goal_id),
                revision = VALUES(revision),
                title = VALUES(title),
                description = VALUES(description),
                status = VALUES(status),
                blocked_reason = VALUES(blocked_reason),
                max_rounds = VALUES(max_rounds),
                rounds_started = VALUES(rounds_started),
                continuation_enabled = VALUES(continuation_enabled),
                created_at = VALUES(created_at),
                updated_at = VALUES(updated_at)
            """)
    void save(GoalStatePO goalStatePO);

    /**
     * 按会话编号查询目标快照。
     * <p>
     * 执行流程：
     * 1. 接收 sessionId；
     * 2. 查询单条目标记录；
     * 3. 返回可选结果。
     */
    @Select("""
            SELECT session_id, goal_id, revision, title, description, status,
                   blocked_reason, max_rounds, rounds_started, continuation_enabled,
                   created_at, updated_at
            FROM harness_goal_state
            WHERE session_id = #{sessionId}
            """)
    Optional<GoalStatePO> findBySessionId(@Param("sessionId") String sessionId);

    /**
     * 查询全部目标快照。
     * <p>
     * 执行流程：
     * 1. 从表中读取全部记录；
     * 2. 按更新时间排序；
     * 3. 返回列表。
     */
    @Select("""
            SELECT session_id, goal_id, revision, title, description, status,
                   blocked_reason, max_rounds, rounds_started, continuation_enabled,
                   created_at, updated_at
            FROM harness_goal_state
            ORDER BY updated_at DESC, session_id ASC
            """)
    List<GoalStatePO> queryAll();
}
