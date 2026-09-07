package cn.xiaofuge.deepseek.harness.infrastructure.adapter.llm;

import cn.xiaofuge.deepseek.harness.domain.llm.adapter.LlmAdapter;
import cn.xiaofuge.deepseek.harness.domain.llm.adapter.ResolvedRetryPolicy;
import cn.xiaofuge.deepseek.harness.domain.llm.model.valobj.FinishReason;
import cn.xiaofuge.deepseek.harness.domain.llm.model.valobj.GenerateOptions;
import cn.xiaofuge.deepseek.harness.domain.llm.model.valobj.LlmFailure;
import cn.xiaofuge.deepseek.harness.domain.llm.model.valobj.LlmErrorCodes;
import cn.xiaofuge.deepseek.harness.domain.llm.model.valobj.StreamChunk;
import cn.xiaofuge.deepseek.harness.domain.llm.adapter.port.ILlmRuntimePort;
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
 * <p>调用流程：模型请求 → HTTP 客户端 → 解析 JSON/SSE → 归一化为领域事件。</p>
 * <p>示例：上游返回 token delta 时，适配器转换为 LLM 增量事件。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public class InMemoryLlmRuntimePort implements ILlmRuntimePort {

    private static final Logger log = LoggerFactory.getLogger(InMemoryLlmRuntimePort.class);

    private final Map<String, LlmAdapter> adapters = new ConcurrentHashMap<>();

    /**
     * 注册模型 Provider 对应的 LLM 适配器。
     * <p>流程：接收 provider 编码和适配器实例 → 写入内存路由表 → 记录注册成功日志。
     * <p>示例：注册 provider=deepseek 后，请求 provider=deepseek 会路由到 DeepSeek 适配器。
     *
     * @param provider 模型提供方编码
     * @param adapter LLM 适配器实现
     */
    @Override
    public void registerAdapter(String provider, LlmAdapter adapter) {
        adapters.put(provider, adapter);
        log.info("Registered LLM adapter for provider: {}", provider);
    }

    /**
     * 取消模型 Provider 的适配器注册。
     * <p>流程：接收 provider 编码 → 从内存路由表移除适配器 → 记录注销成功日志。
     * <p>示例：注销 provider=deepseek 后，后续该 provider 的请求会收到路由失败结果。
     *
     * @param provider 模型提供方编码
     */
    @Override
    public void unregisterAdapter(String provider) {
        adapters.remove(provider);
        log.info("Unregistered LLM adapter for provider: {}", provider);
    }

    /**
     * 列出当前已注册的模型 Provider。
     * <p>流程：遍历内存路由表 → 让适配器生成展示信息 → 汇总为 Provider 列表。
     * <p>示例：注册 deepseek、openai 后返回这两个 provider 的展示信息。
     *
     * @return 已注册 Provider 展示信息列表
     */
    @Override
    public List<LlmAdapter.LlmProviderInfo> listProviders() {
        List<LlmAdapter.LlmProviderInfo> result = new ArrayList<>();
        for (var entry : adapters.entrySet()) {
            result.add(entry.getValue().providerInfo(entry.getKey()));
        }
        return result;
    }

    /**
     * 列出指定 Provider 的可用模型。
     * <p>流程：按 provider 查找适配器 → 委托适配器读取模型目录 → 未找到时返回空列表。
     * <p>示例：provider=deepseek 且适配器存在时，返回 DeepSeek 适配器暴露的模型列表。
     *
     * @param provider 模型提供方编码
     * @return 可用模型列表；Provider 未注册时为空列表
     */
    @Override
    public List<LlmAdapter.LlmModelInfo> listModels(String provider) {
        LlmAdapter adapter = adapters.get(provider);
        return adapter != null ? adapter.listModels(provider) : List.of();
    }

    /**
     * 解析指定 Provider 下的模型元数据。
     * <p>流程：按 provider 查找适配器 → 委托适配器定位模型 → 返回上下文、默认 token 等信息。
     * <p>数据示例：provider=deepseek、model=deepseek-chat 会返回该模型的上下文窗口和默认输出上限。
     *
     * @param provider 模型提供方编码
     * @param model 模型编码
     * @return 已解析的模型信息
     * @throws IllegalArgumentException Provider 未注册适配器时抛出
     */
    @Override
    public LlmAdapter.LlmResolvedModelInfo resolveModel(String provider, String model) {
        LlmAdapter adapter = adapters.get(provider);
        if (adapter == null) {
            String message = "No adapter registered for provider '" + provider
                    + "'. Available providers: " + adapters.keySet()
                    + ". Check providerCode in model settings.";
            log.error("模型路由失败：provider='{}' 未注册适配器，可用 providers={}。"
                            + "请检查 Web 控制台模型设置中的 providerCode。", provider, adapters.keySet());
            throw new IllegalArgumentException(message);
        }
        return adapter.resolveModel(provider, model);
    }

    /**
     * 通过注册的适配器发起流式模型调用。
     * <p>流程：按 provider 查找适配器 → 读取重试策略 → 订阅适配器输出 → 统一包装增量、异常和结束事件。
     * <p>数据示例：provider=deepseek 请求流式补全时，适配器返回的 token delta 会被逐个发布给调用方；
     * Provider 不存在时直接发布一条 Error 结束事件。
     *
     * @param options 模型生成参数
     * @return 可订阅的模型流
     */
    @Override
    public Flow.Publisher<StreamChunk> stream(GenerateOptions options) {
        LlmAdapter adapter = adapters.get(options.provider());
        if (adapter == null) {
            log.error("模型路由失败：provider='{}' 未注册适配器，可用 providers={}。"
                            + "请检查 Web 控制台模型设置中的 providerCode。",
                    options.provider(), adapters.keySet());
            SubmissionPublisher<StreamChunk> publisher = new SubmissionPublisher<>();
            publisher.submit(new StreamChunk.Finish(new FinishReason.Error(new LlmFailure(
                    "No adapter registered for provider '" + options.provider()
                            + "'. Available providers: " + adapters.keySet()
                            + ". Check providerCode in model settings.",
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
    /**
     * 对流式模型调用执行重试。
     * <p>流程：发起流式请求 → 捕获可重试错误 → 按退避策略重建流。</p>
     */
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

    /**
     * 运行With Retry。
     */
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

    /**
     * 处理await Attempt。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @param publisher publisher参数。
     * @return 方法执行结果。
     */
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
            /**
             * on Error。
             * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
             * @param throwable throwable参数。
             */


            @Override public void onError(Throwable throwable) {
                error.set(throwable);
                done.countDown();
            }
            /**
             * on Complete。
             * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
             */


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

    /**
     * 判断Retryable是否成立。
     */
    private boolean isRetryable(LlmFailure failure, ResolvedRetryPolicy policy) {
        if (failure == null) return false;
        if (policy instanceof ResolvedRetryPolicy.Always) return true;
        if (policy instanceof ResolvedRetryPolicy.Normal normal) {
            return normal.retryableCodes().contains(failure.code());
        }
        return false;
    }

    /**
     * 处理sleep。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @param delayMs delay Ms参数。
     * @return 方法执行结果。
     */
    private boolean sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 计算下一次重试延迟。
     * <p>流程：读取基础延迟 → 应用指数退避和上限 → 返回等待毫秒数。</p>
     */
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

    /**
     * 处理with Retry 数量。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @param failure failure参数。
     * @param retryCount retry 数量参数。
     * @return 方法执行结果。
     */
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

    /**
     * AttemptResult 是基础设施适配器，负责外部资源与领域端口对接。
     */
    private record AttemptResult(
            List<StreamChunk> chunks,
            Throwable error,
            LlmFailure failure,
            boolean completed
    ) {}
}
