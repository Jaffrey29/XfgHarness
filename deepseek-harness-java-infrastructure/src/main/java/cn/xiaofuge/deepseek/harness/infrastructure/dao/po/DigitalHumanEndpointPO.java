package cn.xiaofuge.deepseek.harness.infrastructure.dao.po;

import java.time.Instant;

/**
 * 数字人服务端点：连接配置与健康状态。
 * 凭据不落表，只存 credentialRef 引用（明文由调用侧安全存储保管）。
 */
public record DigitalHumanEndpointPO(
        String id,
        String digitalHumanId,
        String endpointType,
        String baseUrl,
        String credentialRef,
        String protocolVersion,
        String agentCard,
        String healthState,
        Instant lastCheckedAt,
        Integer latencyMs
) {
}
