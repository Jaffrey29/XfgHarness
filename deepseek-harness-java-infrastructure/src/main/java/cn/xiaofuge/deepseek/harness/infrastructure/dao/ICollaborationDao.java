package cn.xiaofuge.deepseek.harness.infrastructure.dao;

import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.CollaborationPO.ParticipantPO;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.CollaborationPO.RoomEventPO;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.CollaborationPO.RoomPO;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.CollaborationPO.TaskPO;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 协作域持久化：房间、参与者、任务、事件日志。
 */
@Mapper
public interface ICollaborationDao {

    // ── 房间 ──
    @Insert("""
            INSERT INTO collaboration_room (id, workspace_id, title, objective, orchestration_mode, run_state, created_at)
            VALUES (#{id}, #{workspaceId}, #{title}, #{objective}, #{orchestrationMode}, #{runState}, #{createdAt})
            """)
    void insertRoom(RoomPO po);

    @Select("""
            SELECT id, workspace_id, title, objective, orchestration_mode, run_state, created_at
            FROM collaboration_room WHERE id = #{id}
            """)
    Optional<RoomPO> findRoom(@Param("id") String id);

    @Update("UPDATE collaboration_room SET objective = #{objective} WHERE id = #{id} AND objective IS NULL")
    int fillObjective(@Param("id") String id, @Param("objective") String objective);

    @Update("UPDATE collaboration_room SET run_state = #{runState} WHERE id = #{id}")
    int updateRoomState(@Param("id") String id, @Param("runState") String runState);

    // ── 参与者 ──
    @Insert("""
            INSERT INTO room_participant (id, room_id, digital_human_id, room_role, presence, active_task_label, joined_at)
            VALUES (#{id}, #{roomId}, #{digitalHumanId}, #{roomRole}, #{presence}, #{activeTaskLabel}, #{joinedAt})
            """)
    void insertParticipant(ParticipantPO po);

    @Select("""
            SELECT id, room_id, digital_human_id, room_role, presence, active_task_label, joined_at
            FROM room_participant WHERE room_id = #{roomId} AND digital_human_id = #{digitalHumanId}
            """)
    Optional<ParticipantPO> findParticipant(@Param("roomId") String roomId, @Param("digitalHumanId") String digitalHumanId);

    @Select("""
            SELECT id, room_id, digital_human_id, room_role, presence, active_task_label, joined_at
            FROM room_participant WHERE room_id = #{roomId} ORDER BY joined_at ASC
            """)
    List<ParticipantPO> listParticipants(@Param("roomId") String roomId);

    @Update("""
            UPDATE room_participant SET presence = #{presence}, active_task_label = #{activeTaskLabel}
            WHERE room_id = #{roomId} AND digital_human_id = #{digitalHumanId}
            """)
    int updatePresence(@Param("roomId") String roomId,
                       @Param("digitalHumanId") String digitalHumanId,
                       @Param("presence") String presence,
                       @Param("activeTaskLabel") String activeTaskLabel);

    @Update("DELETE FROM room_participant WHERE room_id = #{roomId} AND digital_human_id = #{digitalHumanId}")
    int removeParticipant(@Param("roomId") String roomId, @Param("digitalHumanId") String digitalHumanId);

    // ── 任务 ──
    @Insert("""
            INSERT INTO collaboration_task (id, room_id, parent_task_id, title, instruction, assigned_to, requested_by, state, depends_on, input_artifacts, output_artifacts, remote_session_id, trace_id, error_summary, created_at, updated_at)
            VALUES (#{id}, #{roomId}, #{parentTaskId}, #{title}, #{instruction}, #{assignedTo}, #{requestedBy}, #{state}, #{dependsOn}, #{inputArtifacts}, #{outputArtifacts}, #{remoteSessionId}, #{traceId}, #{errorSummary}, #{createdAt}, #{updatedAt})
            """)
    void insertTask(TaskPO po);

    @Update("""
            UPDATE collaboration_task SET state = #{state}, remote_session_id = #{remoteSessionId},
                error_summary = #{errorSummary}, updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateTaskState(@Param("id") String id,
                        @Param("state") String state,
                        @Param("remoteSessionId") String remoteSessionId,
                        @Param("errorSummary") String errorSummary,
                        @Param("updatedAt") java.time.Instant updatedAt);

    @Update("UPDATE collaboration_task SET output_artifacts = #{outputArtifacts}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateTaskArtifacts(@Param("id") String id,
                            @Param("outputArtifacts") String outputArtifacts,
                            @Param("updatedAt") java.time.Instant updatedAt);

    @Update("UPDATE collaboration_task SET instruction = #{instruction}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateTaskInstruction(@Param("id") String id,
                              @Param("instruction") String instruction,
                              @Param("updatedAt") java.time.Instant updatedAt);

    @Update("""
            UPDATE collaboration_task SET assigned_to = #{assignedTo}, state = #{state}, remote_session_id = NULL,
                error_summary = NULL, updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateTaskAssignment(@Param("id") String id,
                             @Param("assignedTo") String assignedTo,
                             @Param("state") String state,
                             @Param("updatedAt") java.time.Instant updatedAt);

    @Select("""
            SELECT id, room_id, parent_task_id, title, instruction, assigned_to, requested_by, state, depends_on, input_artifacts, output_artifacts, remote_session_id, trace_id, error_summary, created_at, updated_at
            FROM collaboration_task WHERE id = #{id}
            """)
    Optional<TaskPO> findTask(@Param("id") String id);

    @Select("""
            SELECT id, room_id, parent_task_id, title, instruction, assigned_to, requested_by, state, depends_on, input_artifacts, output_artifacts, remote_session_id, trace_id, error_summary, created_at, updated_at
            FROM collaboration_task WHERE room_id = #{roomId} ORDER BY created_at ASC
            """)
    List<TaskPO> listTasks(@Param("roomId") String roomId);

    // ── 产物 ──
    @Insert("""
            INSERT INTO collaboration_artifact (id, room_id, task_id, producer_id, kind, title, version, content, trace_id, created_at)
            VALUES (#{id}, #{roomId}, #{taskId}, #{producerId}, #{kind}, #{title}, #{version}, #{content}, #{traceId}, #{createdAt})
            """)
    void insertArtifact(cn.xiaofuge.deepseek.harness.infrastructure.dao.po.CollaborationPO.ArtifactPO po);

    @Select("""
            SELECT id, room_id, task_id, producer_id, kind, title, version, content, trace_id, created_at
            FROM collaboration_artifact WHERE room_id = #{roomId} ORDER BY created_at ASC
            """)
    List<cn.xiaofuge.deepseek.harness.infrastructure.dao.po.CollaborationPO.ArtifactPO> listArtifacts(@Param("roomId") String roomId);

    // ── 事件 ──
    @Insert("""
            INSERT INTO room_event (id, room_id, seq, task_id, participant_id, digital_human_id, event_type, occurred_at, visibility, payload, trace_id)
            VALUES (#{id}, #{roomId}, #{seq}, #{taskId}, #{participantId}, #{digitalHumanId}, #{eventType}, #{occurredAt}, #{visibility}, #{payload}, #{traceId})
            """)
    void insertEvent(RoomEventPO po);

    @Select("SELECT COALESCE(MAX(seq), 0) FROM room_event WHERE room_id = #{roomId}")
    long maxEventSeq(@Param("roomId") String roomId);

    @Select("""
            SELECT id, room_id, seq, task_id, participant_id, digital_human_id, event_type, occurred_at, visibility, payload, trace_id
            FROM room_event WHERE room_id = #{roomId} AND seq > #{afterSeq} ORDER BY seq ASC LIMIT #{limit}
            """)
    List<RoomEventPO> listEventsAfter(@Param("roomId") String roomId, @Param("afterSeq") long afterSeq, @Param("limit") int limit);
}
