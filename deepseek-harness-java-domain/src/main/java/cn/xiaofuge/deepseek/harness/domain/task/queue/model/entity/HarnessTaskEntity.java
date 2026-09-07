package cn.xiaofuge.deepseek.harness.domain.task.queue.model.entity;

import cn.xiaofuge.deepseek.harness.domain.shared.model.valobj.HarnessStatusEnumVO;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 排队任务实体。
 * <p>
 * 执行流程：
 * 1. 任务提交成功后生成实体；
 * 2. 实体承载会话、提示词、工具和状态；
 * 3. 仓储负责将其持久化为待执行任务。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class HarnessTaskEntity {

    private final String taskId;
    private final String sessionId;
    private final String prompt;
    private final List<String> enabledTools;
    private final HarnessStatusEnumVO status;
    private final Instant submittedAt;

    /**
     * 处理Harness Task Entity相关逻辑。
     */
    public HarnessTaskEntity(String taskId, String sessionId, String prompt, List<String> enabledTools, HarnessStatusEnumVO status, Instant submittedAt) {
        this.taskId = Objects.requireNonNull(taskId, "taskId");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.prompt = Objects.requireNonNull(prompt, "prompt");
        this.enabledTools = List.copyOf(enabledTools);
        this.status = Objects.requireNonNull(status, "status");
        this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt");
    }

    /**
     * 获取Task Id。
     */
    public String getTaskId() { return taskId; }
    /**
     * 获取Session Id。
     */
    public String getSessionId() { return sessionId; }
    /**
     * 获取Prompt。
     */
    public String getPrompt() { return prompt; }
    /**
     * 获取Enabled Tools。
     */
    public List<String> getEnabledTools() { return enabledTools; }
    /**
     * 获取Status。
     */
    public HarnessStatusEnumVO getStatus() { return status; }
    /**
     * 获取Submitted At。
     */
    public Instant getSubmittedAt() { return submittedAt; }
}
