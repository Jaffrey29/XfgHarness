package cn.xiaofuge.deepseek.harness.api.stream;

import cn.xiaofuge.deepseek.harness.api.dto.StartWorkflowRequestDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface IWorkflowStreamApi {

    SseEmitter stream(StartWorkflowRequestDTO request);
}
