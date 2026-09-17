package cn.xiaofuge.deepseek.harness.infrastructure.digitalhuman;

import cn.xiaofuge.deepseek.harness.infrastructure.dao.IDigitalHumanDao;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.IDigitalHumanEndpointDao;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.DigitalHumanEndpointPO;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.DigitalHumanPO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 数字人目录领域服务：身份 + 端点的 CRUD、Agent Card 探测、健康检查。
 *
 * <p>凭据明文不经过本服务：credentialRef 由调用侧（桌面端安全存储）保管，
 * 探测时仅按需读取，不落库、不进日志。</p>
 */
@Service
public class DigitalHumanService {

    private static final Logger log = LoggerFactory.getLogger(DigitalHumanService.class);
    private static final String PROTOCOL = "dsh.v1";

    private final IDigitalHumanDao humanDao;
    private final IDigitalHumanEndpointDao endpointDao;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DigitalHumanService(IDigitalHumanDao humanDao, IDigitalHumanEndpointDao endpointDao) {
        this.humanDao = humanDao;
        this.endpointDao = endpointDao;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    // ── 目录 ──

    public List<Map<String, Object>> list() {
        List<Map<String, Object>> views = new ArrayList<>();
        for (DigitalHumanPO human : humanDao.listAll()) {
            views.add(toView(human, endpointDao.findByHumanId(human.id()).orElse(null)));
        }
        return views;
    }

    public Map<String, Object> get(String id) {
        DigitalHumanPO human = humanDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("数字人不存在：" + id));
        return toView(human, endpointDao.findByHumanId(id).orElse(null));
    }

    public Map<String, Object> create(Map<String, Object> body) {
        String id = "dh_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Instant now = Instant.now();
        DigitalHumanPO human = new DigitalHumanPO(
                id,
                requireText(body.get("displayName"), "名称不能为空"),
                text(body.get("avatarRef"), "🤖"),
                requireText(body.get("purpose"), "用途描述不能为空"),
                joinTags(body.get("roleTags")),
                text(body.get("themeColor"), "#4160f0"),
                text(body.get("approvalPolicy"), "WRITE_REQUIRES_APPROVAL"),
                clampInt(body.get("concurrencyLimit"), 1, 8, 1),
                now, now);
        humanDao.insert(human);
        upsertEndpoint(id, body, now);
        return get(id);
    }

    public Map<String, Object> update(String id, Map<String, Object> body) {
        DigitalHumanPO existing = humanDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("数字人不存在：" + id));
        DigitalHumanPO merged = new DigitalHumanPO(
                id,
                text(body.get("displayName"), existing.displayName()),
                text(body.get("avatarRef"), existing.avatarRef()),
                text(body.get("purpose"), existing.purpose()),
                body.containsKey("roleTags") ? joinTags(body.get("roleTags")) : existing.roleTags(),
                text(body.get("themeColor"), existing.themeColor()),
                text(body.get("approvalPolicy"), existing.approvalPolicy()),
                body.containsKey("concurrencyLimit")
                        ? clampInt(body.get("concurrencyLimit"), 1, 8, existing.concurrencyLimit())
                        : existing.concurrencyLimit(),
                existing.createdAt(), Instant.now());
        humanDao.update(merged);
        if (body.get("endpoint") instanceof Map<?, ?>) {
            upsertEndpoint(id, body, Instant.now());
        }
        return get(id);
    }

    public void delete(String id) {
        endpointDao.deleteByHumanId(id);
        humanDao.deleteById(id);
    }

    @SuppressWarnings("unchecked")
    private void upsertEndpoint(String humanId, Map<String, Object> body, Instant now) {
        if (!(body.get("endpoint") instanceof Map<?, ?> rawEndpoint)) {
            return;
        }
        Map<String, Object> endpoint = (Map<String, Object>) rawEndpoint;
        String type = text(endpoint.get("type"), "local-dsh");
        String baseUrl = normalizeBaseUrl(text(endpoint.get("baseUrl"), null));
        DigitalHumanEndpointPO po = new DigitalHumanEndpointPO(
                "ep_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                humanId, type, baseUrl,
                text(endpoint.get("credentialRef"), null),
                text(endpoint.get("protocolVersion"), PROTOCOL),
                null,
                text(endpoint.get("healthState"), "local-dsh".equals(type) ? "online" : "unknown"),
                now,
                endpoint.get("latencyMs") instanceof Number number ? number.intValue() : null);
        if (endpointDao.updateByHumanId(po) == 0) {
            endpointDao.insert(po);
        }
    }

    // ── 探测 ──

    /** 探测远端 Agent Card：DSH 走 /.well-known/dsh-agent-card；A2A 走 /.well-known/agent.json。 */
    public Map<String, Object> discover(String baseUrl, String endpointType, String credentialRef, String token) {
        String normalized = normalizeBaseUrl(baseUrl);
        String type = endpointType == null || endpointType.isBlank() ? "remote-dsh" : endpointType;
        Map<String, Object> result = new LinkedHashMap<>();
        if (normalized == null) {
            result.put("reachable", false);
            result.put("error", Map.of("kind", "network", "message", "请填写服务地址"));
            return result;
        }
        if ("a2a".equals(type)) {
            return discoverA2a(normalized, token, result);
        }
        return discoverDsh(normalized, token, result);
    }

    private Map<String, Object> discoverDsh(String normalized, String token, Map<String, Object> result) {
        long startedAt = System.currentTimeMillis();
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(normalized + "/.well-known/dsh-agent-card"))
                    .timeout(Duration.ofSeconds(10))
                    .GET();
            if (token != null && !token.isBlank()) {
                builder.header("Authorization", "Bearer " + token.trim());
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - startedAt;
            result.put("latencyMs", latency);
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                result.put("reachable", false);
                result.put("error", Map.of("kind", "unauthorized", "message", "凭据无效或权限不足"));
                return result;
            }
            if (response.statusCode() == 404) {
                result.put("reachable", false);
                result.put("error", Map.of("kind", "not-found", "message", "该服务未暴露 DSH 能力卡"));
                return result;
            }
            if (response.statusCode() != 200) {
                result.put("reachable", false);
                result.put("error", Map.of("kind", "network", "message", "HTTP " + response.statusCode()));
                return result;
            }
            JsonNode card = objectMapper.readTree(response.body());
            if (!card.hasNonNull("protocol")) {
                result.put("reachable", false);
                result.put("error", Map.of("kind", "incompatible", "message", "响应不是有效的 DSH Agent Card"));
                return result;
            }
            Map<String, Object> suggested = new LinkedHashMap<>();
            suggested.put("displayName", card.path("displayName").asText(null));
            suggested.put("avatarRef", card.path("avatarUrl").asText(null));
            suggested.put("purpose", card.path("purpose").asText(null));
            List<String> tags = new ArrayList<>();
            card.path("roleTags").forEach(node -> tags.add(node.asText()));
            suggested.put("roleTags", tags);
            suggested.put("approvalPolicy", card.path("approvalPolicy").asText("WRITE_REQUIRES_APPROVAL"));
            suggested.put("maxConcurrentTasks", card.path("maxConcurrentTasks").asInt(1));
            result.put("reachable", true);
            result.put("protocolVersion", card.path("protocol").asText());
            result.put("suggested", suggested);
            return result;
        } catch (java.net.http.HttpConnectTimeoutException e) {
            return unreachable(result, startedAt, "network", "连接超时");
        } catch (javax.net.ssl.SSLException e) {
            return unreachable(result, startedAt, "tls", "TLS 证书校验失败：" + e.getMessage());
        } catch (java.net.UnknownHostException e) {
            return unreachable(result, startedAt, "dns", "域名解析失败");
        } catch (Exception e) {
            log.warn("Agent card discover failed for {}: {}", normalized, e.getMessage());
            return unreachable(result, startedAt, "network", "连接失败：" + e.getMessage());
        }
    }

    private Map<String, Object> discoverA2a(String normalized, String token, Map<String, Object> result) {
        long startedAt = System.currentTimeMillis();
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(normalized + "/.well-known/agent.json"))
                    .timeout(Duration.ofSeconds(10))
                    .GET();
            if (token != null && !token.isBlank()) {
                builder.header("Authorization", "Bearer " + token.trim());
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - startedAt;
            result.put("latencyMs", latency);
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                result.put("reachable", false);
                result.put("error", Map.of("kind", "unauthorized", "message", "A2A 凭据无效或权限不足"));
                return result;
            }
            if (response.statusCode() == 404) {
                result.put("reachable", false);
                result.put("error", Map.of("kind", "not-found", "message", "该服务未暴露 A2A Agent Card"));
                return result;
            }
            if (response.statusCode() != 200) {
                result.put("reachable", false);
                result.put("error", Map.of("kind", "network", "message", "HTTP " + response.statusCode()));
                return result;
            }
            JsonNode card = objectMapper.readTree(response.body());
            if (!card.hasNonNull("name") && !card.hasNonNull("description") && !card.has("skills")) {
                result.put("reachable", false);
                result.put("error", Map.of("kind", "incompatible", "message", "响应不是有效的 A2A Agent Card"));
                return result;
            }
            Map<String, Object> suggested = new LinkedHashMap<>();
            suggested.put("displayName", card.path("name").asText(card.path("displayName").asText("A2A Agent")));
            suggested.put("avatarRef", "🤝");
            suggested.put("purpose", card.path("description").asText("通过 A2A 协议接入的外部智能体"));
            List<String> tags = new ArrayList<>();
            card.path("skills").forEach(skill -> {
                if (skill.hasNonNull("name")) tags.add(skill.path("name").asText());
                skill.path("tags").forEach(tag -> tags.add(tag.asText()));
            });
            suggested.put("roleTags", tags.stream().distinct().limit(8).toList());
            suggested.put("approvalPolicy", "WRITE_REQUIRES_APPROVAL");
            suggested.put("maxConcurrentTasks", 1);
            result.put("reachable", true);
            result.put("protocolVersion", card.path("protocolVersion").asText("a2a.v1"));
            result.put("suggested", suggested);
            return result;
        } catch (java.net.http.HttpConnectTimeoutException e) {
            return unreachable(result, startedAt, "network", "连接超时");
        } catch (javax.net.ssl.SSLException e) {
            return unreachable(result, startedAt, "tls", "TLS 证书校验失败：" + e.getMessage());
        } catch (java.net.UnknownHostException e) {
            return unreachable(result, startedAt, "dns", "域名解析失败");
        } catch (Exception e) {
            log.warn("A2A agent card discover failed for {}: {}", normalized, e.getMessage());
            return unreachable(result, startedAt, "network", "连接失败：" + e.getMessage());
        }
    }

    private Map<String, Object> unreachable(Map<String, Object> result, long startedAt, String kind, String message) {
        result.put("reachable", false);
        result.put("latencyMs", System.currentTimeMillis() - startedAt);
        result.put("error", Map.of("kind", kind, "message", message));
        return result;
    }

    // ── 健康检查 ──

    public Map<String, Object> healthCheck(String id, String token) {
        DigitalHumanPO human = humanDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("数字人不存在：" + id));
        DigitalHumanEndpointPO endpoint = endpointDao.findByHumanId(id).orElse(null);
        String state;
        Integer latency = null;
        if (endpoint == null || "local-dsh".equals(endpoint.endpointType())) {
            state = "online"; // 本地数字人随本服务存活
        } else if (endpoint.baseUrl() == null) {
            state = "unknown";
        } else {
            Map<String, Object> probe = discover(endpoint.baseUrl(), endpoint.endpointType(), endpoint.credentialRef(), token);
            state = Boolean.TRUE.equals(probe.get("reachable")) ? "online"
                    : probe.get("error") instanceof Map<?, ?> err && "unauthorized".equals(err.get("kind")) ? "unauthorized"
                    : "offline";
            latency = probe.get("latencyMs") instanceof Number number ? number.intValue() : null;
        }
        endpointDao.updateHealth(id, state, Instant.now(), latency);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("healthState", state);
        result.put("latencyMs", latency);
        return result;
    }

    // ── 视图组装 ──

    private Map<String, Object> toView(DigitalHumanPO human, DigitalHumanEndpointPO endpoint) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", human.id());
        view.put("displayName", human.displayName());
        view.put("avatarRef", human.avatarRef());
        view.put("purpose", human.purpose());
        view.put("roleTags", human.roleTags() == null || human.roleTags().isBlank()
                ? List.of() : List.of(human.roleTags().split(",")));
        view.put("themeColor", human.themeColor());
        view.put("approvalPolicy", human.approvalPolicy());
        view.put("concurrencyLimit", human.concurrencyLimit());
        view.put("createdAt", human.createdAt() == null ? null : human.createdAt().toString());
        Map<String, Object> endpointView = new LinkedHashMap<>();
        endpointView.put("type", endpoint == null ? "local-dsh" : endpoint.endpointType());
        endpointView.put("baseUrl", endpoint == null ? null : endpoint.baseUrl());
        endpointView.put("credentialRef", endpoint == null ? null : endpoint.credentialRef());
        endpointView.put("protocolVersion", endpoint == null ? PROTOCOL : endpoint.protocolVersion());
        endpointView.put("healthState", endpoint == null ? "online" : endpoint.healthState());
        endpointView.put("lastCheckedAt", endpoint != null && endpoint.lastCheckedAt() != null
                ? endpoint.lastCheckedAt().toString() : null);
        endpointView.put("latencyMs", endpoint == null ? null : endpoint.latencyMs());
        view.put("endpoint", endpointView);
        return view;
    }

    // ── 工具 ──

    private String requireText(Object value, String message) {
        String text = text(value, null);
        if (text == null) throw new IllegalArgumentException(message);
        return text;
    }

    private String text(Object value, String fallback) {
        if (value instanceof String string && !string.isBlank()) return string.trim();
        return fallback;
    }

    private String joinTags(Object value) {
        if (value instanceof List<?> list) {
            List<String> tags = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof String tag && !tag.isBlank()) tags.add(tag.trim());
            }
            return String.join(",", tags);
        }
        return text(value, "");
    }

    private int clampInt(Object value, int min, int max, int fallback) {
        if (value instanceof Number number) {
            return Math.max(min, Math.min(max, number.intValue()));
        }
        return fallback;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) return null;
        String normalized = baseUrl.trim().replaceAll("/+$", "");
        return normalized.startsWith("http://") || normalized.startsWith("https://") ? normalized : null;
    }
}
