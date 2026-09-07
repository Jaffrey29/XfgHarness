package cn.xiaofuge.deepseek.harness.domain.workflow.model.entity;

import cn.xiaofuge.deepseek.harness.domain.workflow.model.valobj.WorkflowMeta;
import java.util.Map;
import java.util.Optional;

/**
 * WorkflowStartRequest 表示WorkflowStartRequest请求。
 */
public record WorkflowStartRequest(
        String script,
        WorkflowMeta meta,
        Optional<Object> args,
        Optional<String> subagentProvider,
        Optional<Integer> maxTotalAgents,
        String parentSessionId,
        String parentCwd
) {}
