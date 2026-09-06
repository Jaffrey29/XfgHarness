package cn.walioffice.deepseek.harness.domain.agent.service;

import cn.walioffice.deepseek.harness.domain.agent.model.entity.AgentOptions;

/**
 * Port for creating and managing agent instances.
 * <p>
 * Defined in the domain layer so case-layer code can use it without
 * depending on infrastructure. The infrastructure layer provides the
 * implementation that wires {@link ReactLoopAgent} dependencies.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface AgentFactoryPort {

    /**
     * Create a new agent with a fresh session.
     */
    ReactLoopAgent create(String agentId, AgentOptions options, String cwd);

    /**
     * Resume an agent from an existing session log.
     */
    ReactLoopAgent resume(String agentId, AgentOptions options, String cwd,
                          cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionLog session);

    /**
     * Look up a live agent by id.
     */
    ReactLoopAgent get(String agentId);

    /**
     * Dispose all live agents (graceful shutdown).
     */
    java.util.concurrent.CompletableFuture<Void> disposeAll();
}
