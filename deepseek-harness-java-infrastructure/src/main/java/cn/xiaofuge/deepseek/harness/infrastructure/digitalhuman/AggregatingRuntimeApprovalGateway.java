package cn.xiaofuge.deepseek.harness.infrastructure.digitalhuman;

import cn.xiaofuge.deepseek.harness.domain.agent.contract.outbound.RuntimeApprovalGateway;
import cn.xiaofuge.deepseek.harness.domain.agent.service.run.RuntimeApprovalBroker;
import cn.xiaofuge.deepseek.harness.domain.tool.service.RuntimeApprovalGate.ApprovalRequest;
import cn.xiaofuge.deepseek.harness.domain.tool.service.RuntimeApprovalGate.ApprovalVerdict;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.IDigitalHumanDao;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.IDigitalHumanEndpointDao;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.DigitalHumanEndpointPO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 聚合运行期审批网关：本地 Broker + 远端 DSH 数字人端点。
 *
 * <p>背景：协作把任务委派给远端 DSH 数字人（endpoint.baseUrl 指向另一个 DSH 实例）时，
 * 运行期审批挂起在<b>远端</b> JVM 的 RuntimeApprovalBroker 里并阻塞其 agent 线程；
 * 而桌面端前端只轮询<b>本实例</b>的 {@code /api/harness/approvals/runtime/pending}，
 * 看不到远端待决审批——表现为"数字人请求审批但桌面端不弹审批卡"，
 * 远端 agent 一直阻塞到 10 分钟审批超时被自动拒绝。</p>
 *
 * <p>方案：本网关装饰本地 Broker——{@link #listPending()} 在本地条目之外聚合全部
 * 远端 DSH 端点的待决审批（带 1s 缓存，避免前端 1.5s 轮询打穿远端）；
 * {@link #resolve(String, ApprovalVerdict)} 对非本地 approvalId 转发到来源端点。
 * 凭据使用协作调用期间登记的 token（{@link RemoteEndpointCredentialRegistry}）。</p>
 *
 * <p>注意：本地 agent（含 runLocal 回环）的审批仍走本地 Broker，行为不变；
 * A2A 协议无本审批 REST，不参与聚合。</p>
 */
public class AggregatingRuntimeApprovalGateway implements RuntimeApprovalGateway {

    private static final Logger log = LoggerFactory.getLogger(AggregatingRuntimeApprovalGateway.class);
    private static final String SUCCESS_CODE = "00000";
    /** 远端待决审批缓存窗口：前端 1.5s 轮询 + 多视图并存时避免高频打远端 */
    private static final long REMOTE_CACHE_MS = 1_000L;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

    private final RuntimeApprovalBroker local;
    private final IDigitalHumanEndpointDao endpointDao;
    private final IDigitalHumanDao humanDao;
    private final RemoteEndpointCredentialRegistry credentials;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    /** approvalId -> 来源端点 baseUrl：listPending 时登记，resolve 时据此定向转发 */
    private final ConcurrentMap<String, String> approvalOrigin = new ConcurrentHashMap<>();
    private volatile List<PendingApprovalView> remoteCache = List.of();
    private volatile long remoteCacheAt = 0L;

    public AggregatingRuntimeApprovalGateway(RuntimeApprovalBroker local,
                                             IDigitalHumanEndpointDao endpointDao,
                                             IDigitalHumanDao humanDao,
                                             RemoteEndpointCredentialRegistry credentials) {
        this.local = local;
        this.endpointDao = endpointDao;
        this.humanDao = humanDao;
        this.credentials = credentials;
    }

    /** 本地 agent 的审批挂起：直接委托本地 Broker（agent 与 Broker 同 JVM）。 */
    @Override public ApprovalVerdict requestApproval(ApprovalRequest request) {
        return local.requestApproval(request);
    }

    @Override public boolean resolve(String approvalId, ApprovalVerdict verdict) {
        String origin = approvalOrigin.get(approvalId);
        if (origin == null) {
            // 可能是该审批挂起后首次被查询/索引尚未建立，补一次远端索引再定向
            refreshRemotePending();
            origin = approvalOrigin.get(approvalId);
        }
        if (origin != null) {
            return forwardResolve(origin, approvalId, verdict);
        }
        if (local.resolve(approvalId, verdict)) {
            return true;
        }
        // 索引缺失（如本进程重启后旧前端仍持有裁决请求）：逐个远端端点兜底尝试
        for (RemoteTarget target : remoteTargets()) {
            if (forwardResolve(target.baseUrl(), approvalId, verdict)) return true;
        }
        return false;
    }

    @Override public List<PendingApprovalView> listPending() {
        List<PendingApprovalView> merged = new ArrayList<>(local.listPending());
        for (PendingApprovalView item : remotePending()) {
            boolean duplicated = merged.stream().anyMatch(x -> x.approvalId().equals(item.approvalId()));
            if (!duplicated) merged.add(item);
        }
        return merged;
    }

    @Override public int pendingCount() {
        return local.pendingCount();
    }

    // ══════════════ 远端聚合 ══════════════

    /** 远端 DSH 端点视图：baseUrl 去重后的调用目标（附首个可用 token 与数字人名）。 */
    private record RemoteTarget(String baseUrl, String token, String displayName) {}

    private List<RemoteTarget> remoteTargets() {
        try {
            Map<String, String> nameByHuman = new HashMap<>();
            try {
                humanDao.listAll().forEach(h -> nameByHuman.put(h.id(), h.displayName()));
            } catch (Exception ignored) {
                // 名称仅用于审批卡展示前缀，失败不影响功能
            }
            Map<String, RemoteTarget> byBaseUrl = new LinkedHashMap<>();
            for (DigitalHumanEndpointPO po : endpointDao.findAll()) {
                String baseUrl = po.baseUrl();
                if (baseUrl == null || baseUrl.isBlank()) continue;
                String type = po.endpointType() == null ? "" : po.endpointType();
                // 仅 DSH 协议远端有审批 REST；本地与 A2A 不参与
                if ("local-dsh".equals(type) || "a2a".equals(type)) continue;
                String normalized = baseUrl.trim().replaceAll("/+$", "");
                byBaseUrl.computeIfAbsent(normalized, key -> new RemoteTarget(
                        key,
                        credentials.tokenFor(po.digitalHumanId()),
                        nameByHuman.getOrDefault(po.digitalHumanId(), "")));
            }
            return List.copyOf(byBaseUrl.values());
        } catch (Exception exception) {
            log.warn("[approval] 远端端点枚举失败，本轮仅聚合本地审批：{}", exception.getMessage());
            return List.of();
        }
    }

    private List<PendingApprovalView> remotePending() {
        if (System.currentTimeMillis() - remoteCacheAt < REMOTE_CACHE_MS) {
            return remoteCache;
        }
        refreshRemotePending();
        return remoteCache;
    }

    private synchronized void refreshRemotePending() {
        if (System.currentTimeMillis() - remoteCacheAt < REMOTE_CACHE_MS) return;
        List<PendingApprovalView> merged = new ArrayList<>();
        for (RemoteTarget target : remoteTargets()) {
            for (PendingApprovalView item : fetchRemote(target)) {
                approvalOrigin.put(item.approvalId(), target.baseUrl());
                merged.add(item);
            }
        }
        if (approvalOrigin.size() > 512) approvalOrigin.clear(); // 防索引无限增长
        remoteCache = List.copyOf(merged);
        remoteCacheAt = System.currentTimeMillis();
    }

    private List<PendingApprovalView> fetchRemote(RemoteTarget target) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(target.baseUrl() + "/api/harness/approvals/runtime/pending"))
                    .timeout(REQUEST_TIMEOUT)
                    .GET();
            if (target.token() != null) {
                builder.header("Authorization", "Bearer " + target.token());
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return List.of();
            JsonNode root = objectMapper.readTree(response.body());
            if (!SUCCESS_CODE.equals(root.path("code").asText())) return List.of();
            // 审批卡展示带上"谁在请求"——房间内多个数字人时用户需要知道裁决对象
            String label = target.displayName() == null || target.displayName().isBlank()
                    ? "" : "「" + target.displayName() + "」";
            List<PendingApprovalView> items = new ArrayList<>();
            for (JsonNode node : root.path("data")) {
                items.add(new PendingApprovalView(
                        node.path("approvalId").asText(""),
                        node.path("sessionId").asText(""),
                        node.path("toolName").asText(""),
                        label + node.path("displayCommand").asText(""),
                        objectMapper.convertValue(node.path("arguments"),
                                new TypeReference<Map<String, Object>>() {}),
                        node.path("createdAt").asText("")));
            }
            return items;
        } catch (Exception exception) {
            log.debug("[approval] 远端待决审批获取失败 base={}：{}", target.baseUrl(), exception.getMessage());
            return List.of();
        }
    }

    private boolean forwardResolve(String baseUrl, String approvalId, ApprovalVerdict verdict) {
        try {
            String token = null;
            for (RemoteTarget target : remoteTargets()) {
                if (target.baseUrl().equals(baseUrl)) {
                    token = target.token();
                    break;
                }
            }
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/harness/approvals/runtime/"
                            + approvalId + "/resolve"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(Map.of("verdict", verdict.name()))));
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return false;
            JsonNode root = objectMapper.readTree(response.body());
            return SUCCESS_CODE.equals(root.path("code").asText())
                    && root.path("data").path("resolved").asBoolean(false);
        } catch (Exception exception) {
            log.warn("[approval] 裁决转发失败 base={} approval={}：{}", baseUrl, approvalId, exception.getMessage());
            return false;
        }
    }
}
