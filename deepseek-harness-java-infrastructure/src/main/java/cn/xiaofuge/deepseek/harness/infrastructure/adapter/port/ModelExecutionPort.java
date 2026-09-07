package cn.xiaofuge.deepseek.harness.infrastructure.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.llm.adapter.port.ILlmRuntimePort;
import cn.xiaofuge.deepseek.harness.domain.llm.model.valobj.FinishReason;
import cn.xiaofuge.deepseek.harness.domain.llm.model.valobj.GenerateOptions;
import cn.xiaofuge.deepseek.harness.domain.llm.model.valobj.StreamChunk;
import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingQueryService;
import cn.xiaofuge.deepseek.harness.domain.task.execution.adapter.port.IModelExecutionPort;
import cn.xiaofuge.deepseek.harness.domain.task.execution.model.valobj.ModelExecutionResultVO;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * Adapts the execution-domain port to the unified LLM runtime.
 * Routes through the same adapter chain used by the agent loop.
 * <p>调用流程：领域端口 → 基础设施适配器 → 外部系统。</p>
 * <p>示例：适配器只实现端口契约，业务规则保留在 domain。</p>
 */
@Component
public class ModelExecutionPort implements IModelExecutionPort {

    private final ILlmRuntimePort llmRuntimePort;
    private final IModelSettingQueryService modelSettingService;

    public ModelExecutionPort(ILlmRuntimePort llmRuntimePort, IModelSettingQueryService modelSettingService) {
        this.llmRuntimePort = llmRuntimePort;
        this.modelSettingService = modelSettingService;
    }

    @Override
    public ModelExecutionResultVO execute(String profileCode, String prompt) {
        var setting = modelSettingService.findByProfileCode(profileCode)
                .filter(s -> s.enabled())
                .orElseThrow(() -> new IllegalStateException(
                        "model setting is missing or disabled for profile: " + profileCode));

        GenerateOptions options = new GenerateOptions(
                "deepseek",
                setting.modelCode(),
                null,
                List.of(Message.createUser(
                        List.of(new TextBlock(prompt)),
                        new cn.xiaofuge.deepseek.harness.domain.model.entity.MessageSource.UserSource())),
                null, null, null, null, null, null, null,
                setting.baseUrl(),
                setting.apiKeyRef()
        );

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> content = new AtomicReference<>("");
        AtomicReference<String> responseModel = new AtomicReference<>(setting.modelCode());
        AtomicReference<Throwable> error = new AtomicReference<>(null);

        llmRuntimePort.stream(options).subscribe(new Flow.Subscriber<>() {
            /**
             * on Subscribe。
             * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
             * @param s s参数。
             */

            private Flow.Subscription subscription;
            @Override public void onSubscribe(Flow.Subscription s) {
                this.subscription = s;
                s.request(Long.MAX_VALUE);
            /**
             * on Next。
             * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
             * @param item item参数。
             */

            }
            @Override public void onNext(StreamChunk item) {
                if (item instanceof StreamChunk.TextDelta delta) {
                    content.updateAndGet(c -> c + delta.text());
                } else if (item instanceof StreamChunk.Finish finish) {
                    latch.countDown();
                    latch.countDown();
                }
            /**
             * on Error。
             * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
             * @param t t参数。
             */

            }
            @Override public void onError(Throwable t) {
                error.set(t);
                latch.countDown();
            /**
             * on Complete。
             * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
             */

            }
            @Override public void onComplete() { latch.countDown(); }
        });

        try {
            if (!latch.await(120, TimeUnit.SECONDS)) {
                throw new IllegalStateException("model execution timed out for profile: " + profileCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("model execution interrupted", e);
        }

        if (error.get() != null) {
            throw new IllegalStateException("model execution failed: " + error.get().getMessage(), error.get());
        }

        return new ModelExecutionResultVO(setting.providerCode(), responseModel.get(), content.get());
    }
}
