package cn.walioffice.deepseek.harness.infrastructure.adapter.llm;

import cn.walioffice.deepseek.harness.domain.llm.adapter.LlmAdapter;
import cn.walioffice.deepseek.harness.domain.llm.adapter.ResolvedRetryPolicy;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.FinishReason;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.GenerateOptions;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.LlmFailure;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.LlmErrorCodes;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.StreamChunk;
import cn.walioffice.deepseek.harness.domain.llm.adapter.port.ILlmRuntimePort;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 内存版大模型运行时端口实现。
 * <p>
 * 执行流程：
 * 1. 维护 provider 到适配器的映射；
 * 2. 根据 provider 路由到具体模型适配器；
 * 3. 将异常统一包装成结束 chunk 返回。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class InMemoryLlmRuntimePort implements ILlmRuntimePort {

    private static final Logger log = LoggerFactory.getLogger(InMemoryLlmRuntimePort.class);

    private final Map<String, LlmAdapter> adapters = new ConcurrentHashMap<>();

    @Override
    public void registerAdapter(String provider, LlmAdapter adapter) {
        adapters.put(provider, adapter);
        log.info("Registered LLM adapter for provider: {}", provider);
    }

    @Override
    public void unregisterAdapter(String provider) {
        adapters.remove(provider);
        log.info("Unregistered LLM adapter for provider: {}", provider);
    }

    @Override
    public List<LlmAdapter.LlmProviderInfo> listProviders() {
        List<LlmAdapter.LlmProviderInfo> result = new ArrayList<>();
        for (var entry : adapters.entrySet()) {
            result.add(entry.getValue().providerInfo(entry.getKey()));
        }
        return result;
    }

    @Override
    public List<LlmAdapter.LlmModelInfo> listModels(String provider) {
        LlmAdapter adapter = adapters.get(provider);
        return adapter != null ? adapter.listModels(provider) : List.of();
    }

    @Override
    public LlmAdapter.LlmResolvedModelInfo resolveModel(String provider, String model) {
        LlmAdapter adapter = adapters.get(provider);
        if (adapter == null) {
            throw new IllegalArgumentException("No adapter registered for provider: " + provider);
        }
        return adapter.resolveModel(provider, model);
    }

    @Override
    public Flow.Publisher<StreamChunk> stream(GenerateOptions options) {
        LlmAdapter adapter = adapters.get(options.provider());
        if (adapter == null) {
            SubmissionPublisher<StreamChunk> publisher = new SubmissionPublisher<>();
            publisher.submit(new StreamChunk.Finish(new FinishReason.Error(new LlmFailure(
                    "No adapter registered for provider: " + options.provider(),
                    LlmErrorCodes.UNKNOWN
            ))));
            publisher.close();
            return publisher;
        }

        ResolvedRetryPolicy policy = adapter.retryPolicy(options.provider());
        if (policy != null) {
            return retryingStream(adapter, options, policy);
        }

        // 包装适配器的流式输出，统一异常表现
        SubmissionPublisher<StreamChunk> wrapper = new SubmissionPublisher<>();
        adapter.stream(options).subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(StreamChunk item) {
                wrapper.submit(item);
            }

            @Override
            public void onError(Throwable throwable) {
                LlmFailure failure = new LlmFailure(
                        throwable.getMessage() != null ? throwable.getMessage() : "LLM adapter failed",
                        LlmErrorCodes.UNKNOWN
                );
                log.error("LLM stream error for provider={}", options.provider(), throwable);
                wrapper.submit(new StreamChunk.Finish(new FinishReason.Error(failure)));
                wrapper.close();
            }

            @Override
            public void onComplete() {
                wrapper.close();
            }
        });

        return wrapper;
    }

    private Flow.Publisher<StreamChunk> retryingStream(
            LlmAdapter adapter,
            GenerateOptions options,
            ResolvedRetryPolicy policy
    ) {
        SubmissionPublisher<StreamChunk> wrapper = new SubmissionPublisher<>();
        Thread retryThread = new Thread(() -> runWithRetry(adapter, options, policy, wrapper));
        retryThread.setName("llm-retry-" + options.provider());
        retryThread.setDaemon(true);
        retryThread.start();
        return wrapper;
    }

    private void runWithRetry(
            LlmAdapter adapter,
            GenerateOptions options,
            ResolvedRetryPolicy policy,
            SubmissionPublisher<StreamChunk> wrapper
    ) {
        int attemptCount = policy instanceof ResolvedRetryPolicy.Normal normal
                ? normal.maxRetries()
                : Integer.MAX_VALUE;
        AtomicInteger attempt = new AtomicInteger();

        while (true) {
            attempt.incrementAndGet();
            AttemptResult result = awaitAttempt(adapter.stream(options));

            if (result.completed() && isRetryable(result.failure(), policy)) {
                if (attempt.get() > attemptCount) {
                    LlmFailure exhausted = withRetryCount(result.failure(), attempt.get() - 1);
                    wrapper.submit(new StreamChunk.Finish(new FinishReason.Error(exhausted)));
                    wrapper.close();
                    return;
                }
                Integer maxAttempts = policy instanceof ResolvedRetryPolicy.Normal normal
                        ? normal.maxRetries()
                        : null;
                wrapper.submit(new StreamChunk.Retry(
                        attempt.get(),
                        maxAttempts,
                        result.failure() == null ? "model request failed" : result.failure().message()
                ));
                if (!sleep(retryDelayMs(policy, attempt.get(), result.failure()))) {
                    wrapper.close();
                    return;
                }
                continue;
            }

            result.chunks().forEach(wrapper::submit);
            if (result.error() != null) {
                log.error("LLM stream error for provider={}", options.provider(), result.error());
                wrapper.closeExceptionally(result.error());
            } else {
                wrapper.close();
            }
            return;
        }
    }

    private AttemptResult awaitAttempt(Flow.Publisher<StreamChunk> publisher) {
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        java.util.List<StreamChunk> chunks = java.util.Collections.synchronizedList(new ArrayList<>());
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<FinishReason> finish = new java.util.concurrent.atomic.AtomicReference<>();

        publisher.subscribe(new Flow.Subscriber<>() {
            @Override public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override public void onNext(StreamChunk item) {
                chunks.add(item);
                if (item instanceof StreamChunk.Finish finishChunk) {
                    finish.set(finishChunk.reason());
                }
            }

            @Override public void onError(Throwable throwable) {
                error.set(throwable);
                done.countDown();
            }

            @Override public void onComplete() {
                done.countDown();
            }
        });

        try {
            done.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            error.set(exception);
        }
        FinishReason terminalFinish = finish.get();
        LlmFailure failure = terminalFinish instanceof FinishReason.Error errorFinish
                ? errorFinish.failure()
                : null;
        return new AttemptResult(chunks, error.get(), failure, terminalFinish != null);
    }

    private boolean isRetryable(LlmFailure failure, ResolvedRetryPolicy policy) {
        if (failure == null) return false;
        if (policy instanceof ResolvedRetryPolicy.Always) return true;
        if (policy instanceof ResolvedRetryPolicy.Normal normal) {
            return normal.retryableCodes().contains(failure.code());
        }
        return false;
    }

    private boolean sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private long retryDelayMs(
            ResolvedRetryPolicy policy,
            int failedAttempt,
            LlmFailure failure
    ) {
        long localDelay = (long) Math.min(
                policy.maxDelayMs(),
                (double) policy.initialDelayMs() * Math.pow(2, failedAttempt - 1)
        );
        Long providerDelay = failure == null ? null : failure.providerRetryAfterMs();
        if (providerDelay != null && providerDelay > 0 && providerDelay <= policy.maxDelayMs()) {
            return providerDelay;
        }
        double jitter = 1 + (ThreadLocalRandom.current().nextDouble(2) - 1) * policy.jitterRatio();
        return Math.max(0, Math.round(localDelay * jitter));
    }

    private LlmFailure withRetryCount(LlmFailure failure, int retryCount) {
        String suffix = "（已重试 " + retryCount + " 次）";
        return new LlmFailure(
                failure.message() + suffix,
                failure.code(),
                failure.status(),
                failure.providerRetryAfterMs(),
                failure.requestId()
        );
    }

    private record AttemptResult(
            List<StreamChunk> chunks,
            Throwable error,
            LlmFailure failure,
            boolean completed
    ) {}
}
