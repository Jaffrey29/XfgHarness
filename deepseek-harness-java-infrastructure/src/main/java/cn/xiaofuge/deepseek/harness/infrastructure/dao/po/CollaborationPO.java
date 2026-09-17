package cn.xiaofuge.deepseek.harness.infrastructure.dao.po;

import java.time.Instant;

/**
 * 协作域持久化记录集合。
 */
public final class CollaborationPO {

    private CollaborationPO() {
    }

    /** 协作房间 */
    public record RoomPO(
            String id,
            String workspaceId,
            String title,
            String objective,
            String orchestrationMode,
            String runState,
            Instant createdAt
    ) {
    }

    /** 房间参与者 */
    public record ParticipantPO(
            String id,
            String roomId,
            String digitalHumanId,
            String roomRole,
            String presence,
            String activeTaskLabel,
            Instant joinedAt
    ) {
    }

    /** 协作任务 */
    public record TaskPO(
            String id,
            String roomId,
            String parentTaskId,
            String title,
            String instruction,
            String assignedTo,
            String requestedBy,
            String state,
            String dependsOn,
            String inputArtifacts,
            String outputArtifacts,
            String remoteSessionId,
            String traceId,
            String errorSummary,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    /** 协作产物 */
    public record ArtifactPO(
            String id,
            String roomId,
            String taskId,
            String producerId,
            String kind,
            String title,
            Integer version,
            String content,
            String traceId,
            Instant createdAt
    ) {
    }

    /** 统一房间事件 */
    public record RoomEventPO(
            String id,
            String roomId,
            Long seq,
            String taskId,
            String participantId,
            String digitalHumanId,
            String eventType,
            Instant occurredAt,
            String visibility,
            String payload,
            String traceId
    ) {
    }
}
