package cn.xiaofuge.deepseek.harness.infrastructure.digitalhuman;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * 远端数字人凭据登记表（内存态）。
 * <p>
 * 协作把任务委派给远端 DSH 数字人时会携带该数字人的访问 token（随消息下发）；
 * 运行期审批聚合（{@link AggregatingRuntimeApprovalGateway}）需要在任务执行期间
 * 回源远端实例查询/裁决待决审批，这里登记最近一次协作调用实际使用的 token，
 * 供聚合器按数字人取用。token 不落盘，进程重启后由下一次协作调用重新登记。
 */
@Component
public class RemoteEndpointCredentialRegistry {

    private final ConcurrentMap<String, String> tokenByHumanId = new ConcurrentHashMap<>();

    /** 登记数字人最近一次协作调用使用的远端 token（空值忽略）。 */
    public void remember(String digitalHumanId, String token) {
        if (digitalHumanId == null || digitalHumanId.isBlank()) return;
        if (token == null || token.isBlank()) return;
        tokenByHumanId.put(digitalHumanId, token);
    }

    /** 取数字人的远端 token；未登记过返回 null（远端未配置密钥时无需凭据）。 */
    public String tokenFor(String digitalHumanId) {
        return digitalHumanId == null ? null : tokenByHumanId.get(digitalHumanId);
    }
}
