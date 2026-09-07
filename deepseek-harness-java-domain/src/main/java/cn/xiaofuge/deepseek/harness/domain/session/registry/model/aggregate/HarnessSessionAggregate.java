package cn.xiaofuge.deepseek.harness.domain.session.registry.model.aggregate;

import cn.xiaofuge.deepseek.harness.domain.shared.model.valobj.HarnessStatusEnumVO;
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

    /**
     * 处理Harness Session Aggregate相关逻辑。
     */
    public HarnessSessionAggregate(String sessionId, String workspaceId, String profileCode, Instant createdAt) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId");
        this.profileCode = Objects.requireNonNull(profileCode, "profileCode");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.status = HarnessStatusEnumVO.CREATED;
    }

    /**
     * 获取Session Id。
     */
    public String getSessionId() { return sessionId; }
    /**
     * 获取Workspace Id。
     */
    public String getWorkspaceId() { return workspaceId; }
    /**
     * 获取Profile Code。
     */
    public String getProfileCode() { return profileCode; }
    /**
     * 获取Created At。
     */
    public Instant getCreatedAt() { return createdAt; }
    /**
     * 获取Execution Mode。
     */
    public String getExecutionMode() { return executionMode; }
    /**
     * 获取Resolved Model。
     */
    public String getResolvedModel() { return resolvedModel; }
    /**
     * 获取Model Code。
     */
    public String getModelCode() { return resolvedModel; }
    /**
     * 获取Status。
     */
    public HarnessStatusEnumVO getStatus() { return status; }
    /**
     * 获取Last Activity At。
     */
    public Instant getLastActivityAt() { return lastActivityAt != null ? lastActivityAt : createdAt; }

    /**
     * 处理prepare相关逻辑。
     */
    public void prepare(String executionMode, String resolvedModel) {
        this.executionMode = Objects.requireNonNull(executionMode, "executionMode");
        this.resolvedModel = Objects.requireNonNull(resolvedModel, "resolvedModel");
    }

    /**
     * 处理touch Activity相关逻辑。
     */
    public void touchActivity() {
        this.lastActivityAt = Instant.now();
    }

    /**
     * 设置Last Activity At。
     */
    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    /**
     * 处理change Status相关逻辑。
     */
    public void changeStatus(HarnessStatusEnumVO status) {
        this.status = Objects.requireNonNull(status, "status");
    }
}
