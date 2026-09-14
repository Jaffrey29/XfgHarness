package cn.xiaofuge.deepseek.harness.api.gateway;

import cn.xiaofuge.deepseek.harness.api.gateway.dto.GatewayStreamRequestDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface IGatewayStreamApi {

    SseEmitter stream(GatewayStreamRequestDTO request);
}
