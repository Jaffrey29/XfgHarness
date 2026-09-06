package cn.xiaofuge.deepseek.harness.infrastructure.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.task.execution.adapter.port.IModelExecutionPort;
import cn.xiaofuge.deepseek.harness.domain.task.execution.model.valobj.ModelExecutionResultVO;
import cn.xiaofuge.deepseek.harness.infrastructure.gateway.DeepSeekGatewayService;
import org.springframework.stereotype.Component;

/**
 * Adapts the execution-domain port to the concrete model gateway.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Component
public class ModelExecutionPort implements IModelExecutionPort {

    private final DeepSeekGatewayService deepSeekGatewayService;

    public ModelExecutionPort(DeepSeekGatewayService deepSeekGatewayService) {
        this.deepSeekGatewayService = deepSeekGatewayService;
    }

    @Override
    public ModelExecutionResultVO execute(String profileCode, String prompt) {
        return deepSeekGatewayService.execute(profileCode, prompt);
    }
}
