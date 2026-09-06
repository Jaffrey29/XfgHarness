package cn.xiaofuge.deepseek.harness.domain.session.event.model.entity;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable metadata stored at the head of a session event log.
 * <p>
 * Analogous to the TS {@code SessionHeader}. Contains information needed
 * to correctly reconstruct a session from its event log.
 *
 * @author 小傅哥
 * @website bugstack.cn
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
     * Create a new session header with default version.
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
     * Create a session header for a subagent fork.
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
