package cn.xiaofuge.deepseek.harness.domain.agentrun;

import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionLog;
import java.util.concurrent.CompletableFuture;

/**
 * AgentRunFactoryPort 表示创建、复用和释放智能体运行的领域端口。
 *
 * <p>调用流程：case 层请求运行 → 领域端口 → infrastructure 装配真实依赖。</p>
 */
public interface AgentRunFactoryPort {

    AgentRun create(String agentId, AgentOptions options, String cwd);

    AgentRun resume(String agentId, AgentOptions options, String cwd, SessionLog session);

    AgentRun get(String agentId);

    CompletableFuture<Void> disposeAll();
}
