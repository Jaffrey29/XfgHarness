package cn.xiaofuge.deepseek.harness.domain.tool.service;

import cn.xiaofuge.deepseek.harness.domain.model.entity.*;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ContentBlock;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolCallBlock;
import cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj.HookOutput;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionLog;
import cn.xiaofuge.deepseek.harness.domain.session.event.service.SessionEventFactory;
import cn.xiaofuge.deepseek.harness.domain.tool.adapter.ToolRegistry;
import cn.xiaofuge.deepseek.harness.domain.tool.model.valobj.ToolErrorCodes;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolExecutionMode;
import cn.xiaofuge.deepseek.harness.domain.hooks.IHookService;
import cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj.HookDialect;
import cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj.HookPoint;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Domain 层不引入外部日志，由调用方负责记录
// 注：为满足运行期可观测性需求，此处破例引入 slf4j 记录工具执行的关键步骤

/**
 * Schedules one assistant step's tool calls.
 * Mirrors TS {@code executeToolCalls} from {@code tool-calls.ts}.
 *
 * <p>Exclusive calls form barriers; parallel calls use a bounded rolling pool
 * and are reclassified before start. Dispatch may overlap, while results and
 * result context remain model-ordered. Abort or an internal scheduler failure
 * stops replenishment and drains started calls.
 *
 * <p>Abort records synthetic error results for skipped calls so replay stays
 * valid. A terminal scheduler failure preserves already-recorded
 * {@code tool/call} events without fabricating results.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public final class ToolCallExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolCallExecutor.class);
    // Domain 层不持有日志器

    private final ToolRegistry registry;
    private final SessionLog session;
    private final int maxParallel;
    private final ToolArgumentsParser argumentsParser;
    private volatile IHookService hookService;
    private volatile RuntimeApprovalGate approvalGate = RuntimeApprovalGate.allowAll();

    /**
     * @param registry    工具注册表
     * @param session     会话日志，用于持久化事件追加
     * @param maxParallel 每个步骤中可并行的最大在途调用数
     */
    public ToolCallExecutor(ToolRegistry registry, SessionLog session, int maxParallel) {
        this(registry, session, maxParallel, ToolArgumentsParser.identity());
    }

    public ToolCallExecutor(
            ToolRegistry registry,
            SessionLog session,
            int maxParallel,
            ToolArgumentsParser argumentsParser
    ) {
        this.registry = Objects.requireNonNull(registry);
        this.session = Objects.requireNonNull(session);
        this.argumentsParser = Objects.requireNonNull(argumentsParser);
        if (maxParallel < 1) throw new IllegalArgumentException("maxParallel must be >= 1");
        this.maxParallel = maxParallel;
    }

    public void setHookService(IHookService hookService) {
        this.hookService = hookService;
    }

    /**
     * 注入运行期审批门禁。每次派发工具前都会调用 {@link RuntimeApprovalGate#check}，
     * 被 DENY 的调用记录合成失败结果（不执行工具），保持会话回放一致。
     */
    public void setApprovalGate(RuntimeApprovalGate approvalGate) {
        this.approvalGate = approvalGate == null ? RuntimeApprovalGate.allowAll() : approvalGate;
    }

    /**
     * 运行期审批判定结果：携带裁决详情（而非布尔），以区分 deny 与 cancel。
     */
    private record ApprovalOutcome(boolean denied, boolean cancelled) {}

    private ApprovalOutcome checkApproval(String toolName, Object args) {
        RuntimeApprovalGate.Decision decision = approvalGate.check(toolName, args);
        return new ApprovalOutcome(decision == RuntimeApprovalGate.Decision.DENY, false);
    }

    private boolean shouldBlock(String toolName) {
        if (hookService == null) return false;
        var outcome = hookService.runHooks(
                HookDialect.CLAUDE_CODE, HookPoint.PRE_TOOL_USE, toolName,
                "{\"tool\":\"" + toolName + "\"}", Map.of());
        return outcome.decision()
                .map(d -> d == HookOutput.HookDecision.BLOCK
                       || d == HookOutput.HookDecision.DENY)
                .orElse(false);
    }

    private void afterTool(String toolName) {
        if (hookService == null) return;
        hookService.runHooks(
                HookDialect.CLAUDE_CODE, HookPoint.POST_TOOL_USE, toolName,
                "{\"tool\":\"" + toolName + "\"}", Map.of());
    }

    /**
     * 按当前并发模式调度一个助手步骤中的工具调用。
     *
     * @param turn         current turn number
     * @param step         current step number
     * @param toolCalls    assistant calls in model order
     * @param abortSignal  shared abort flag
     * @param acceptContext accepts committed result context for the next step boundary
     * @return whether any committed result concluded the turn
     */
    public boolean execute(
            long turn,
            long step,
            List<ToolCallBlock> toolCalls,
            AtomicBoolean abortSignal,
            java.util.function.Consumer<Message> acceptContext
    ) {
        if (toolCalls.isEmpty()) return false;
        log.info("[Tool] Turn#{} Step#{} 调度 {} 个工具调用：{}",
                turn, step, toolCalls.size(),
                toolCalls.stream().map(ToolCallBlock::name).toList());

        // 规划调用
        List<PlannedCall> planned = new ArrayList<>();
        for (ToolCallBlock block : toolCalls) {
            planned.add(new PlannedCall(block, argumentsParser.parse(block.arguments())));
        }

        int next = 0;
        boolean concluded = false;
        while (next < planned.size()) {
            PlannedCall first = planned.get(next);
            ToolExecutionMode mode = registry.executionMode(
                    ToolExecutionInput.root(first.block.id(), first.block.name(), first.execArgs));
            List<PlannedCall> group = mode == ToolExecutionMode.PARALLEL
                    ? planned.subList(next, planned.size())
                    : List.of(first);
            GroupOutcome outcome = runGroup(turn, step, group, mode, abortSignal, acceptContext);
            next += outcome.consumed;
            concluded |= outcome.concluded;
            if (outcome.aborted) {
                for (int i = next; i < planned.size(); i++) {
                    appendSkippedToolCall(turn, step, planned.get(i).block);
                }
                return concluded;
            }
        }
        return concluded;
    }

    // ── 分组执行器 ────────────────────────────────────────────────

    private GroupOutcome runGroup(
            long turn,
            long step,
            List<PlannedCall> group,
            ToolExecutionMode mode,
            AtomicBoolean abortSignal,
            java.util.function.Consumer<Message> acceptContext
    ) {
        int groupSize = group.size();
        Slot[] slots = new Slot[groupSize];
        long[] callSeqs = new long[groupSize];
        Arrays.fill(callSeqs, -1);
        int nextToStart = 0;
        int committed = 0;
        int started = 0;
        boolean aborted = abortSignal.get();
        boolean concluded = false;
        boolean schedulerFailure = false;

        // 记录在途 Future
        Map<Integer, CompletableFuture<Integer>> inFlight = new ConcurrentHashMap<>();

        try {
            // 填充线程池
            while (!aborted && nextToStart < groupSize && inFlight.size() < maxParallel) {
                // 有序提交后重新读取后续调用模式
                if (nextToStart > 0 && mode == ToolExecutionMode.PARALLEL) {
                    PlannedCall nextCall = group.get(nextToStart);
                    ToolExecutionMode recheck = registry.executionMode(
                            ToolExecutionInput.root(nextCall.block.id(), nextCall.block.name(), nextCall.execArgs));
                    if (recheck != ToolExecutionMode.PARALLEL) break;
                }

                int index = nextToStart;
                PlannedCall call = group.get(index);

                // PRE_TOOL_USE Hook：被拦截时跳过工具
                if (shouldBlock(call.block.name())) {
                    log.warn("[Tool] Turn#{} Step#{} 工具 {} 被 PreToolUse Hook 拦截，跳过执行",
                            turn, step, call.block.name());
                    callSeqs[index] = appendToolCall(turn, step, call.block);
                    started++;
                    slots[index] = new Slot(null, ToolExecutionResult.fail(
                            "Blocked by PreToolUse hook", "HOOK_BLOCKED"), false);
                    nextToStart++;
                    continue;
                }

                // 运行期审批门禁：被 DENY 时不执行工具，记录合成失败结果
                if (checkApproval(call.block.name(), call.execArgs).denied()) {
                    log.warn("[Tool] Turn#{} Step#{} 工具 {} 需审批但未获批准（DENY），跳过执行",
                            turn, step, call.block.name());
                    callSeqs[index] = appendToolCall(turn, step, call.block);
                    started++;
                    slots[index] = new Slot(null, ToolExecutionResult.fail(
                            "Tool \"" + call.block.name()
                                    + "\" requires approval and was not approved for this session",
                            "APPROVAL_REQUIRED"), false);
                    nextToStart++;
                    continue;
                }

                // 追加 tool/call 事件
                callSeqs[index] = appendToolCall(turn, step, call.block);
                started++;

                // 准备并派发调用
                log.debug("[Tool] Turn#{} Step#{} 派发工具 {} ...", turn, step, call.block.name());
                ToolExecution exec = ToolExecution.from(
                        ToolExecutionInput.root(call.block.id(), call.block.name(), call.execArgs));
                ToolRunContext ctx = new ToolRunContext(exec);

                try {
                    var defOpt = registry.lookup(call.block.name());
                    if (defOpt.isEmpty()) {
                        log.warn("[Tool] Turn#{} Step#{} 未知工具 {}，记录失败", turn, step, call.block.name());
                        slots[index] = new Slot(ctx, ToolExecutionResult.fail(
                                "unknown tool \"" + call.block.name() + "\"",
                                ToolErrorCodes.UNKNOWN_TOOL), false);
                    } else {
                        // 通过 CompletableFuture 同步派发
                        CompletableFuture<ToolExecutionResult> future = defOpt.get()
                                .execute(call.execArgs, ctx);
                        int finalIndex = index;
                        CompletableFuture<Integer> tracked = future.handle((result, error) -> {
                            if (error != null) {
                                slots[finalIndex] = new Slot(ctx,
                                        ToolExecutionResult.fail(error.getMessage(), "TOOL_ERROR"), false);
                            } else {
                                afterTool(call.block.name());
                                boolean needsPost = true;
                                slots[finalIndex] = new Slot(ctx, result, needsPost);
                            }
                            return finalIndex;
                        });
                        inFlight.put(index, tracked);
                        tracked.whenComplete((ignored, throwable) -> inFlight.remove(finalIndex));
                        // 非并行模式下，等待该调用完成后再继续
                        if (mode != ToolExecutionMode.PARALLEL) {
                            future.join();
                        }
                    }
                } catch (Exception e) {
                    slots[index] = new Slot(ctx,
                            ToolExecutionResult.fail(e.getMessage(), "TOOL_ERROR"), false);
                }

                nextToStart++;
                if (abortSignal.get()) aborted = true;

                // 按顺序提交已就绪槽位
                committed = commitReady(turn, step, group, slots, callSeqs, committed, acceptContext);
            }

            // 等待全部在途调用完成
            while (!inFlight.isEmpty()) {
                CompletableFuture.allOf(inFlight.values().toArray(new CompletableFuture[0]))
                        .join();
                committed = commitReady(turn, step, group, slots, callSeqs, committed, acceptContext);
                if (abortSignal.get()) aborted = true;

                // 尝试继续填充
                while (!aborted && nextToStart < groupSize && inFlight.size() < maxParallel) {
                    if (nextToStart > 0 && mode == ToolExecutionMode.PARALLEL) {
                        PlannedCall nextCall = group.get(nextToStart);
                        ToolExecutionMode recheck = registry.executionMode(
                                ToolExecutionInput.root(nextCall.block.id(), nextCall.block.name(), nextCall.execArgs));
                        if (recheck != ToolExecutionMode.PARALLEL) break;
                    }
                    int index = nextToStart;
                    PlannedCall call = group.get(index);
                    callSeqs[index] = appendToolCall(turn, step, call.block);
                    started++;
                    // 运行期审批门禁（补流路径）
                    if (checkApproval(call.block.name(), call.execArgs).denied()) {
                        slots[index] = new Slot(null, ToolExecutionResult.fail(
                                "Tool \"" + call.block.name()
                                        + "\" requires approval and was not approved for this session",
                                "APPROVAL_REQUIRED"), false);
                        nextToStart++;
                        committed = commitReady(turn, step, group, slots, callSeqs, committed, acceptContext);
                        continue;
                    }
                    ToolExecution exec = ToolExecution.from(
                            ToolExecutionInput.root(call.block.id(), call.block.name(), call.execArgs));
                    ToolRunContext ctx = new ToolRunContext(exec);
                    try {
                        var defOpt = registry.lookup(call.block.name());
                        if (defOpt.isEmpty()) {
                            slots[index] = new Slot(ctx, ToolExecutionResult.fail(
                                    "unknown tool", ToolErrorCodes.UNKNOWN_TOOL), false);
                        } else {
                            CompletableFuture<ToolExecutionResult> future = defOpt.get()
                                    .execute(call.execArgs, ctx);
                            int finalIndex = index;
                            future.whenComplete((result, error) -> {
                                if (error != null) {
                                    slots[finalIndex] = new Slot(ctx,
                                            ToolExecutionResult.fail(error.getMessage(), "TOOL_ERROR"), false);
                                } else {
                                    slots[finalIndex] = new Slot(ctx, result, true);
                                }
                                inFlight.remove(finalIndex);
                            });
                            inFlight.put(index, future.thenApply(v -> finalIndex));
                        }
                    } catch (Exception e) {
                        slots[index] = new Slot(ctx,
                                ToolExecutionResult.fail(e.getMessage(), "TOOL_ERROR"), false);
                    }
                    nextToStart++;
                    committed = commitReady(turn, step, group, slots, callSeqs, committed, acceptContext);
                }
            }

        } catch (Exception e) {
            schedulerFailure = true;
            // 清空在途调用
            for (var f : inFlight.values()) {
                try { f.join(); } catch (Exception ignored) {}
            }
        }

        if (aborted) {
            for (int i = started; i < groupSize; i++) {
                appendSkippedToolCall(turn, step, group.get(i).block);
            }
            return new GroupOutcome(groupSize, true, concluded);
        }

        // 提交剩余已完成槽位
        while (committed < groupSize && slots[committed] != null) {
            committed = commitOne(turn, step, group.get(committed), slots[committed],
                    callSeqs[committed], committed, acceptContext);
        }

        return new GroupOutcome(committed, false, concluded);
    }

    // ── 提交辅助方法 ──────────────────────────────────────────────

    private int commitReady(
            long turn, long step, List<PlannedCall> group,
            Slot[] slots, long[] callSeqs,
            int committed,
            java.util.function.Consumer<Message> acceptContext
    ) {
        while (committed < group.size() && slots[committed] != null) {
            committed = commitOne(turn, step, group.get(committed), slots[committed],
                    callSeqs[committed], committed, acceptContext);
        }
        return committed;
    }

    private int commitOne(
            long turn, long step, PlannedCall call, Slot slot,
            long callSeq, int index,
            java.util.function.Consumer<Message> acceptContext
    ) {
        ToolExecutionResult result = slot.result;

        // 按需执行 finalizeContent
        if (slot.needsPost) {
            var defOpt = registry.lookup(call.block.name());
            if (defOpt.isPresent()) {
                var finalized = defOpt.get().finalizeContent(slot.ctx.execution(), result);
                if (finalized.isPresent()) {
                    // 替换内容块
                    if (result instanceof ToolExecutionResult.Success s) {
                        result = new ToolExecutionResult.Success(s.value(), finalized.get(),
                                s.meta(), s.additionalContexts(), s.concludesTurn());
                    } else if (result instanceof ToolExecutionResult.Failure f) {
                        result = new ToolExecutionResult.Failure(f.error(), finalized.get(),
                                f.meta(), f.additionalContexts());
                    }
                }
            }
        }

        // 合并延迟上下文
        List<Message> deferred = slot.ctx.drainDeferredContexts();
        if (!deferred.isEmpty()) {
            if (result instanceof ToolExecutionResult.Success s) {
                var combined = new ArrayList<>(s.additionalContexts());
                combined.addAll(deferred);
                result = new ToolExecutionResult.Success(s.value(), s.content(),
                        s.meta(), combined, s.concludesTurn());
            } else if (result instanceof ToolExecutionResult.Failure f) {
                var combined = new ArrayList<>(f.additionalContexts());
                combined.addAll(deferred);
                result = new ToolExecutionResult.Failure(f.error(), f.content(),
                        f.meta(), combined);
            }
        }

        // 追加 tool/result 事件
        appendToolResult(turn, step, call.block, result, callSeq);
        if (result.isError()) {
            String errDetail = result instanceof ToolExecutionResult.Failure f ? f.error().message() : "unknown";
            log.warn("[Tool] Turn#{} Step#{} 工具 {} 执行失败：{}（callId={}）",
                    turn, step, call.block.name(), errDetail, call.block.id());
        } else {
            log.info("[Tool] Turn#{} Step#{} 工具 {} 执行成功（callId={}）",
                    turn, step, call.block.name(), call.block.id());
        }

        // 接收额外上下文
        for (Message ctx : result.additionalContexts()) {
            acceptContext.accept(ctx);
        }

        return index + 1;
    }

    // ── 持久化事件追加 ────────────────────────────────────────────

    private long appendToolCall(long turn, long step, ToolCallBlock block) {
        var event = SessionEventFactory.toolCall(turn, step, block.id(), block.name(), block.arguments());
        session.append(event);
        return event.seq();
    }

    private void appendToolResult(
            long turn, long step, ToolCallBlock block,
            ToolExecutionResult result, long callSeq
    ) {
        // 构造结果内容块
        List<ContentBlock> content = result.content();
        boolean isError = result.isError();

        // 构造工具结果消息
        Message message = Message.createToolResult(block.id(), content, isError);

        // 存在错误信息时提取错误详情
        Map<String, Object> errorInfo = null;
        if (result instanceof ToolExecutionResult.Failure f && f.error().info() != null) {
            errorInfo = Map.of(
                    "name", f.error().info().name(),
                    "code", f.error().info().code()
            );
        }

        // 存在元数据时提取 meta
        Map<String, Object> meta = null;
        if (result instanceof ToolExecutionResult.Success s) {
            meta = s.meta();
        } else if (result instanceof ToolExecutionResult.Failure f) {
            meta = f.meta();
        }

        var event = SessionEventFactory.toolResult(turn, step, message, errorInfo, meta);
        session.append(event);
    }

    private void appendSkippedToolCall(long turn, long step, ToolCallBlock block) {
        long callSeq = appendToolCall(turn, step, block);
        var result = ToolExecutionResult.fail(
                "Error: tool call aborted before dispatch",
                ToolErrorCodes.ABORTED_BEFORE_DISPATCH
        );
        appendToolResult(turn, step, block, result, callSeq);
    }

    // ── 工具方法 ──────────────────────────────────────────────────

    // ── 内部类型 ──────────────────────────────────────────────────

    private record PlannedCall(ToolCallBlock block, Object execArgs) {}

    private record Slot(ToolRunContext ctx, ToolExecutionResult result, boolean needsPost) {}

    private record GroupOutcome(int consumed, boolean aborted, boolean concluded) {}
}
