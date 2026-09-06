package cn.walioffice.deepseek.harness.infrastructure.adapter.agent;

import cn.walioffice.deepseek.harness.domain.agent.ask.UserQuestionsPort;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Simple in-memory implementation of {@link UserQuestionsPort}.
 * <p>
 * Stores pending questions in a map keyed by request id. When a user
 * response arrives (via REST endpoint or other channel), it completes
 * the future. This enables blocking-wait semantics for the agent loop
 * without requiring WebSocket or SSE infrastructure.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class InMemoryUserQuestionsPort implements UserQuestionsPort {

    private final ConcurrentMap<String, CompletableFuture<UserAnswer>> pending = new ConcurrentHashMap<>();
    private final long defaultTimeoutMs;

    public InMemoryUserQuestionsPort() {
        this(300_000); // 5 min default timeout
    }

    public InMemoryUserQuestionsPort(long defaultTimeoutMs) {
        this.defaultTimeoutMs = defaultTimeoutMs;
    }

    @Override
    public UserAnswer ask(UserQuestionRequest request) {
        String requestId = UUID.randomUUID().toString();
        CompletableFuture<UserAnswer> future = new CompletableFuture<>();
        pending.put(requestId, future);
        try {
            return future.get(defaultTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            pending.remove(requestId);
            throw new RuntimeException("User question timed out", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pending.remove(requestId);
            throw new RuntimeException("User question interrupted", e);
        } catch (Exception e) {
            pending.remove(requestId);
            throw new RuntimeException("User question failed", e);
        } finally {
            pending.remove(requestId);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * Submit a user's answer to a pending question.
     *
     * @param requestId the request id (returned from {@link #getPendingRequestId})
     * @param answer    the user's answer
     * @return true if the answer was accepted
     */
    public boolean submitAnswer(String requestId, UserAnswer answer) {
        CompletableFuture<UserAnswer> future = pending.get(requestId);
        if (future != null) {
            return future.complete(answer);
        }
        return false;
    }

    /**
     * Get the current pending request ids (for UI polling).
     */
    public Map<String, CompletableFuture<UserAnswer>> getPending() {
        return Map.copyOf(pending);
    }
}
