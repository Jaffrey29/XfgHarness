package cn.xiaofuge.deepseek.harness.domain.workflow.model.entity;

import cn.xiaofuge.deepseek.harness.domain.workflow.model.valobj.WorkflowMeta;
import java.util.Optional;

/**
 * What a caller asks for when starting a workflow run.
 *
 * @param script           plain JS script body (ends with return <json-value>)
 * @param meta             workflow identity block
 * @param args             optional input exposed to the script as the args global
 * @param subagentProvider optional engine-wide child-provider override
 * @param maxTotalAgents   optional per-run total-child ceiling
 * @param parentSessionId  parent session id (parent of every child)
 * @param parentCwd        parent working directory
 *
 * @author 小傅哥
 * @website bugstack.cn
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
