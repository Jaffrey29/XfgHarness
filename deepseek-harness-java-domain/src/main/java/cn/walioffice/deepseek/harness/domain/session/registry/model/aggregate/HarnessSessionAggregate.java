package cn.walioffice.deepseek.harness.domain.session.registry.model.aggregate;

import cn.walioffice.deepseek.harness.domain.shared.model.valobj.HarnessStatusEnumVO;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents the durable lifecycle state for one harness session.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class HarnessSessionAggregate {

    private final String sessionId;
    private final String workspaceId;
    private final String profileCode;
    private final Instant createdAt;
    private String executionMode;
    private String resolvedModel;
    private HarnessStatusEnumVO status;
    private Instant lastActivityAt;

    public HarnessSessionAggregate(String sessionId, String workspaceId, String profileCode, Instant createdAt) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId");
        this.profileCode = Objects.requireNonNull(profileCode, "profileCode");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.status = HarnessStatusEnumVO.CREATED;
    }

    public String getSessionId() { return sessionId; }
    public String getWorkspaceId() { return workspaceId; }
    public String getProfileCode() { return profileCode; }
    public Instant getCreatedAt() { return createdAt; }
    public String getExecutionMode() { return executionMode; }
    public String getResolvedModel() { return resolvedModel; }
    public String getModelCode() { return resolvedModel; }
    public HarnessStatusEnumVO getStatus() { return status; }
    public Instant getLastActivityAt() { return lastActivityAt != null ? lastActivityAt : createdAt; }

    public void prepare(String executionMode, String resolvedModel) {
        this.executionMode = Objects.requireNonNull(executionMode, "executionMode");
        this.resolvedModel = Objects.requireNonNull(resolvedModel, "resolvedModel");
    }

    public void touchActivity() {
        this.lastActivityAt = Instant.now();
    }

    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public void changeStatus(HarnessStatusEnumVO status) {
        this.status = Objects.requireNonNull(status, "status");
    }
}
