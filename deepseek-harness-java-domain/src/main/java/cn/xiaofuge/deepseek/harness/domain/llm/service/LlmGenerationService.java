package cn.xiaofuge.deepseek.harness.domain.llm.service;

import cn.xiaofuge.deepseek.harness.domain.llm.adapter.port.ILlmRuntimePort;
import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;
import cn.xiaofuge.deepseek.harness.domain.model.entity.MessageSource;
import cn.xiaofuge.deepseek.harness.domain.llm.model.valobj.GenerateOptions;
import cn.xiaofuge.deepseek.harness.domain.llm.model.valobj.StreamChunk;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Synchronous helper for assembling a complete LLM response from a stream.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class LlmGenerationService {

    private final ILlmRuntimePort llmRuntimePort;

    public LlmGenerationService(ILlmRuntimePort llmRuntimePort) {
        this.llmRuntimePort = llmRuntimePort;
    }

    public CompletableFuture<Message> generate(GenerateOptions options) {
        var assembler = new BlockAssembler();
        var done = new CountDownLatch(1);
        var error = new AtomicReference<Throwable>();

        llmRuntimePort.stream(options).subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(StreamChunk item) {
                assembler.push(item);
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                done.countDown();
            }

            @Override
            public void onComplete() {
                done.countDown();
            }
        });

        return CompletableFuture.supplyAsync(() -> {
            try {
                done.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(exception);
            }
            if (error.get() != null) {
                throw new RuntimeException(error.get());
            }
            return assembler.message(new MessageSource.ModelMessageSource(options.provider(), options.model()));
        });
    }
}
