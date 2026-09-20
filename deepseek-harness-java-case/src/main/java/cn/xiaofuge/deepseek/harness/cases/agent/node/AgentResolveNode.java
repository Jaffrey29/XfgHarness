package cn.xiaofuge.deepseek.harness.cases.agent.node;

import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.agent.factory.AgentMessageDynamicContext;
import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.agent.service.run.AgentRun;
import cn.xiaofuge.deepseek.harness.domain.agent.service.run.PersistingSessionLog;
import cn.xiaofuge.deepseek.harness.domain.agent.contract.inbound.AgentRunLifecycle;
import cn.xiaofuge.deepseek.harness.domain.agent.model.valobj.AgentStatus;
import cn.xiaofuge.deepseek.harness.domain.agent.model.valobj.ApprovalModeVO;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.service.AgentModelSettingResolver;
import cn.xiaofuge.deepseek.harness.domain.session.event.adapter.repository.ISessionEventStore;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionHeader;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionLog;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 解析节点：获取或创建目标 Agent。
 *
 * <p>调用流程：HTTP/API 门面 → AgentUseCase → AgentMessageFactory → 策略树 → ReactLoopAgent；会话事件写入 SessionEventLog。</p>
 * <p>示例：调用方传入 agentId、用户消息和可选模型配置，返回助手文本与工具调用结果。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service("agentMessageResolveNode")
public class AgentResolveNode extends AbstractAgentMessageNode {

    private static final Logger log = LoggerFactory.getLogger(AgentResolveNode.class);

    @Autowired
    private AgentRunLifecycle agentFactory;

    @Autowired
    private AgentModelSettingResolver modelSettingResolver;

    /** 会话事件持久化存储：实例缺失时用于从事件日志恢复历史上下文；未装配时跳过恢复 */
    @Autowired(required = false)
    private ISessionEventStore sessionEventStore;

    @Autowired
    @Qualifier("agentMessageIntentNode")
    private AgentIntentNode intentNode;

    @Autowired
    private cn.xiaofuge.deepseek.harness.domain.sandbox.service.SandboxExtraRootsRegistry sandboxExtraRootsRegistry;

    /**
     * 解析或创建本次消息要使用的 Agent。
     * <p>执行流程：按 agentId 查缓存；命中则复用，未命中则用默认 provider/model 和工作目录创建。
     * 解析结果只写入上下文，本节点不产生最终响应。
     * <p>数据示例：agentId="a-1"，request.model="deepseek-chat"，cwd=null，则使用当前进程目录创建 Agent。
     */
    @Override
    protected AgentMessageResponseDTO doApply(AgentMessageRequestDTO request,
                                           AgentMessageDynamicContext ctx) throws Exception {
        // 桌面端把项目下授权的工程目录随请求传入，workspace-write 沙箱据此放行；
        // Agent 的 cwd（会话归属工程）必须一并授权：FS 工具的相对路径现在解析到 cwd，
        // 否则写入 cwd 下的文件会被沙箱拦截（cwd 未必出现在 sandboxRoots 里）
        java.util.List<String> sandboxRoots = request.sandboxRoots();
        if (sandboxRoots != null) {
            String requestCwd = request.cwd();
            if (requestCwd != null && !requestCwd.isBlank() && !sandboxRoots.contains(requestCwd)) {
                java.util.List<String> withCwd = new java.util.ArrayList<>(sandboxRoots);
                withCwd.add(requestCwd);
                sandboxRoots = withCwd;
            }
            sandboxExtraRootsRegistry.update(sandboxRoots);
        }
        AgentRun agent = agentFactory.get(request.agentId());
        if (agent != null) {
            // 流式请求互斥：Agent 的流式 sink 是单实例字段，运行中再挂第二个请求的 sink
            // 会覆盖第一个请求的回调（前一条 SSE 流丢失增量）。非流式路径消息会进 Inbox
            // 排队，不受影响；数字人房间任务每次用全新 agentId（collab-<ts>），也不会命中。
            boolean streaming = ctx.getDeltaSink() != null || ctx.getToolCallEventSink() != null;
            if (streaming && agent.status() == AgentStatus.RUNNING) {
                throw new IllegalStateException(
                        "Agent=" + request.agentId() + " 正在执行上一条消息，请等待完成或先停止当前生成");
            }
        }
        if (agent == null) {
            AgentOptions options = buildAgentOptions(request);
            String workDir = request.cwd() != null ? request.cwd() : System.getProperty("user.dir");
            // 实例缺失（桌面应用/服务重启）≠ 会话丢失：事件日志里有完整历史。
            // 直接 create 会开全新空会话，模型看到"继续"却没有任何上文；
            // 这里先尝试按 agentId 恢复最近一次持久化会话，恢复失败再走全新会话。
            SessionLog restoredSession = findPersistedSession(request.agentId());
            if (restoredSession != null) {
                log.info("[Resolve] Agent={} 内存实例缺失，从事件日志恢复会话 sessionId={}（事件数={}）",
                        request.agentId(), restoredSession.sessionId(), restoredSession.size());
                agent = agentFactory.resume(request.agentId(), options, workDir, restoredSession);
            } else {
                log.info("[Resolve] Agent={} 不存在，创建新实例（channel={} model={} cwd={}）",
                        request.agentId(), options.channelCode(), options.model(), workDir);
                agent = agentFactory.create(request.agentId(), options, workDir);
            }
            agentFactory.configureApprovalMode(request.agentId(), approvalMode(request));
        } else {
            log.debug("[Resolve] Agent={} 已存在，复用实例", request.agentId());
            agent.updateOptions(buildAgentOptions(request));
            agentFactory.configureApprovalMode(request.agentId(), approvalMode(request));
            // 同步 cwd：桌面端同一 agentId 可能在不同会话/工程间复用，
            // 不同步会导致 FS 工具把相对路径写到旧工程目录
            agentFactory.updateCwd(request.agentId(), request.cwd());
        }
        ctx.attachSinks(agent);
        ctx.setAgent(agent);
        return null;
    }

    @Override
    public StrategyHandler<AgentMessageRequestDTO, AgentMessageDynamicContext, AgentMessageResponseDTO>
    /**
     * 获取Next。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param request 请求对象。
     * @param ctx ctx参数。
     * @return 获取到的Next。
     */

    getNext(AgentMessageRequestDTO request, AgentMessageDynamicContext ctx) {
        return intentNode;
    }

    /**
     * 按 agentId 查找最近一次持久化会话并重建内存日志（实例缺失时的上下文恢复）。
     * <p>listHeaders 按 created_at 倒序返回，第一个匹配即最近会话；header 的 agentPreset
     * 槽位存的是创建时的 agentId（见 AgentRunFactory.create）。恢复出的日志用
     * PersistingSessionLog 承载，后续 turn 的新事件继续落库。
     * 无持久化存储 / 无匹配会话 / 恢复异常时返回 null（调用方走全新会话路径）。
     */
    private SessionLog findPersistedSession(String agentId) {
        if (sessionEventStore == null) {
            return null;
        }
        try {
            for (SessionHeader header : sessionEventStore.listHeaders(200)) {
                if (!agentId.equals(header.agentPreset())) {
                    continue;
                }
                java.util.List<SessionEvent> events = sessionEventStore.readAll(header.sessionId());
                if (events.isEmpty()) {
                    continue;
                }
                PersistingSessionLog restored = new PersistingSessionLog(header.sessionId(), header, sessionEventStore);
                restored.restore(events);
                return restored;
            }
        } catch (Exception exception) {
            log.warn("[Resolve] Agent={} 尝试从事件日志恢复会话失败，改用全新会话：{}",
                    agentId, exception.getMessage());
        }
        return null;
    }

    /**
     * 将 HTTP 请求字段转换为 Agent 运行参数。
     * <p>统一使用 {@link AgentModelSettingResolver}：请求显式值优先，其次数据库默认配置，
     * 最后才使用 application.yml 的模型兜底。
     * <p>approvalMode 也要写入 options：ApprovalAwareFsPort/ApprovalAwareShellExecutor
     * 依据 options.approvalMode 判断是否放开沙箱（FULL_OPEN 走不受限端口），
     * 只配置 gate 不同步 options 会导致完全开放模式下 fs_write 仍被沙箱拦截。
     */
    private AgentOptions buildAgentOptions(AgentMessageRequestDTO request) {
        return modelSettingResolver.resolve(request.channelCode(), request.maxTokens())
                .withReasoningEffort(request.reasoningEffort())
                .withApprovalMode(approvalMode(request));
    }

    private ApprovalModeVO approvalMode(AgentMessageRequestDTO request) {
        return ApprovalModeVO.from(request.approvalMode());
    }
}
