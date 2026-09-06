package cn.xiaofuge.deepseek.harness.api.service.query;

import cn.xiaofuge.deepseek.harness.api.response.settings.ModelSettingResponse;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingQueryService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 模型设置查询适配器。
 * <p>
 * 执行流程：
 * 1. 调用领域查询服务读取模型设置 VO；
 * 2. 将 VO 映射为接口返回的响应对象；
 * 3. 返回给 Trigger 层直接输出。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class HarnessModelSettingQueryApi {

    private final IModelSettingQueryService modelSettingQueryService;

    public HarnessModelSettingQueryApi(IModelSettingQueryService modelSettingQueryService) {
        this.modelSettingQueryService = modelSettingQueryService;
    }

    /**
     * 查询全部模型设置。
     * <p>
     * 执行流程：
     * 1. 拉取领域层的模型设置列表；
     * 2. 逐条转换为 `ModelSettingResponse`；
     * 3. 返回给上层控制器。
     */
    public List<ModelSettingResponse> queryAll() {
        return modelSettingQueryService.queryAll().stream()
                .map(setting -> new ModelSettingResponse(
                        setting.profileCode(),
                        setting.providerCode(),
                        setting.modelCode(),
                        setting.baseUrl(),
                        setting.apiKeyRef(),
                        setting.enabled(),
                        setting.updatedAt()
                ))
                .toList();
    }
}
