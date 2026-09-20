package cn.xiaofuge.deepseek.harness.domain.agent.contract.inbound;

import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.agent.model.valobj.ApprovalModeVO;
import cn.xiaofuge.deepseek.harness.domain.agent.service.run.AgentRun;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionLog;
import java.util.concurrent.CompletableFuture;

/**
 * AgentRunLifecycle 表示创建、复用和释放智能体运行的入口契约。
 *
 * <p>调用流程：case 层请求运行 → Agent 领域服务创建或复用运行实例。</p>
 */
public interface AgentRunLifecycle {

    AgentRun create(String agentId, AgentOptions options, String cwd);

    AgentRun resume(String agentId, AgentOptions options, String cwd, SessionLog session);

    AgentRun get(String agentId);

    void configureApprovalMode(String agentId, ApprovalModeVO mode);

    /**
     * 热更新 Agent 工作目录。Agent 实例被复用且请求换了工程目录时调用，
     * 使 FS 工具的相对路径解析跟随最新 cwd。
     *
     * @param agentId Agent 标识。
     * @param cwd 新工作目录，null/空忽略。
     */
    void updateCwd(String agentId, String cwd);

    CompletableFuture<Void> disposeAll();
}
