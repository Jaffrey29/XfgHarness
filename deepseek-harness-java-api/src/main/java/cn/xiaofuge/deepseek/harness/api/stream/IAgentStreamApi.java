package cn.xiaofuge.deepseek.harness.api.stream;

import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageRequestDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface IAgentStreamApi {

    SseEmitter sendMessageStreaming(AgentMessageRequestDTO request);
}
