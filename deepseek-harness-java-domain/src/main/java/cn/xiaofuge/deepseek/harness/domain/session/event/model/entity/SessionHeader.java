package cn.xiaofuge.deepseek.harness.domain.session.event.model.entity;

import java.time.Instant;
import java.util.Objects;

/**
 * SessionHeader 表示会话SessionHeader。
 */
public record SessionHeader(
        int version,           // session format version (currently 0)
        String sessionId,      // unique session identifier
        Instant createdAt,     // epoch ms, non-negative
        String cwd,            // working directory (or null)
        String parentSession,  // parent session for fork/subagent (or null)
        int seedLength,        // number of seed events inherited from parent
        String origin,         // "subagent" or null
        int delegationDepth,   // recursion depth budget (persisted across restarts)
        String agentPreset     // determines tools & prompts for this session
) {
    public static final int SESSION_FORMAT_VERSION = 0;

    public SessionHeader {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(createdAt, "createdAt");
        if (version < 0) {
            throw new IllegalArgumentException("version must be non-negative: " + version);
        }
        if (seedLength < 0) {
            throw new IllegalArgumentException("seedLength must be non-negative: " + seedLength);
        }
        if (delegationDepth < 0) {
            throw new IllegalArgumentException("delegationDepth must be non-negative: " + delegationDepth);
        }
    }

    /**
     * 创建领域对象。
     * <p>流程：校验入参 → 执行领域动作 → 返回处理结果。</p>
     * @param sessionId 会话ID。
     * @param cwd 工作目录。
     * @return 领域处理结果。
     */
    public static SessionHeader create(
            String sessionId,
            Instant createdAt,
            String cwd,
            String agentPreset
    ) {
        return new SessionHeader(
                SESSION_FORMAT_VERSION,
                sessionId,
                createdAt,
                cwd,
                null,   // parentSession
                0,      // seedLength
                null,   // origin
                0,      // delegationDepth
                agentPreset
        );
    }

    /**
     * 处理forSubagent。
     * <p>流程：校验入参 → 执行领域动作 → 返回处理结果。</p>
     * @param sessionId 会话ID。
     * @param cwd 工作目录。
     * @param seedLength seedLength。
     * @param agentPreset agentPreset。
     * @return 领域处理结果。
     */
    public static SessionHeader forSubagent(
            String sessionId,
            Instant createdAt,
            String cwd,
            String parentSession,
            int seedLength,
            int delegationDepth,
            String agentPreset
    ) {
        return new SessionHeader(
                SESSION_FORMAT_VERSION,
                sessionId,
                createdAt,
                cwd,
                parentSession,
                seedLength,
                "subagent",
                delegationDepth,
                agentPreset
        );
    }
}
