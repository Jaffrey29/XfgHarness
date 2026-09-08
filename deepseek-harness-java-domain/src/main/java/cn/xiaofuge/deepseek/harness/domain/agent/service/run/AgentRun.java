package cn.xiaofuge.deepseek.harness.domain.agent.service.run;

import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.agent.service.Agent;

import java.util.concurrent.CompletableFuture;

/**
 * AgentRun 表示一次可交互的智能体运行。
 *
 * <p>调用流程：创建/恢复运行 → 接收输入 → 驱动 ReAct 循环 → 取消或等待空闲。</p>
 */
public interface AgentRun extends Agent {

    /**
     * 热更新下一次模型调用使用的运行参数。
     *
     * @param options 领域配置。
     */
    void updateOptions(AgentOptions options);

    /**
     * 绑定流式文本增量接收器。
     */
    void setStreamDeltaSink(java.util.function.Consumer<String> sink);

    /**
     * 绑定流式推理增量接收器。
     */
    void setStreamReasoningSink(java.util.function.Consumer<String> sink);

    /**
     * 绑定工具调用事件接收器。
     */
    void setStreamToolCallSink(java.util.function.Consumer<
            cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolCallBlock> sink);

    void setStreamToolEventSink(java.util.function.Consumer<
            cn.xiaofuge.deepseek.harness.domain.tool.service.ToolCallExecutor.ToolLifecycleEvent> sink);

    /**
     * 释放当前运行占用的资源。
     */
    CompletableFuture<Void> dispose();
}
