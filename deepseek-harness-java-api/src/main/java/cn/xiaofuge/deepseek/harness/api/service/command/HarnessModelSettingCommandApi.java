package cn.xiaofuge.deepseek.harness.api.service.command;

import cn.xiaofuge.deepseek.harness.api.command.settings.SaveModelSettingRequest;
import cn.xiaofuge.deepseek.harness.api.response.settings.ModelSettingResponse;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingCommandService;
import org.springframework.stereotype.Service;

/**
 * 模型设置写入适配器。
 * <p>
 * 执行流程：
 * 1. 接收前端提交的写请求；
 * 2. 调用领域命令服务完成保存或删除；
 * 3. 将领域结果转换为接口响应。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class HarnessModelSettingCommandApi {

    private final IModelSettingCommandService modelSettingCommandService;

    public HarnessModelSettingCommandApi(IModelSettingCommandService modelSettingCommandService) {
        this.modelSettingCommandService = modelSettingCommandService;
    }

    /**
     * 保存或更新一条模型设置。
     * <p>
     * 执行流程：
     * 1. 读取请求参数；
     * 2. 调用领域命令服务落库；
     * 3. 将保存后的领域对象转为响应对象返回。
     */
    public ModelSettingResponse save(SaveModelSettingRequest request) {
        var setting = modelSettingCommandService.save(
                request.profileCode(),
                request.providerCode(),
                request.modelCode(),
                request.baseUrl(),
                request.apiKeyRef(),
                request.enabled()
        );
        return new ModelSettingResponse(
                setting.profileCode(),
                setting.providerCode(),
                setting.modelCode(),
                setting.baseUrl(),
                setting.apiKeyRef(),
                setting.enabled(),
                setting.updatedAt()
        );
    }

    /**
     * 删除指定 profile 的模型设置。
     * <p>
     * 执行流程：
     * 1. 接收 profileCode；
     * 2. 交给领域命令服务执行删除；
     * 3. 由控制器返回空响应。
     */
    public void delete(String profileCode) {
        modelSettingCommandService.deleteByProfileCode(profileCode);
    }
}
