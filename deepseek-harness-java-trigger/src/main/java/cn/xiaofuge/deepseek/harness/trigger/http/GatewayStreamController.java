package cn.xiaofuge.deepseek.harness.trigger.http;

import cn.xiaofuge.deepseek.harness.api.gateway.IGatewayStreamApi;
import cn.xiaofuge.deepseek.harness.api.gateway.dto.GatewayStreamRequestDTO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class GatewayStreamController {

    private final IGatewayStreamApi gatewayStreamApi;

    public GatewayStreamController(IGatewayStreamApi gatewayStreamApi) {
        this.gatewayStreamApi = gatewayStreamApi;
    }

    @PostMapping(value = "/api/gateway/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody GatewayStreamRequestDTO request) {
        return gatewayStreamApi.stream(request);
    }
}
