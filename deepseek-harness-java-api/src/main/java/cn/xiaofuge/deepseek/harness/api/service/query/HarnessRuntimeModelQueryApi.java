package cn.xiaofuge.deepseek.harness.api.service.query;

import cn.xiaofuge.deepseek.harness.api.response.runtime.AvailableModelResponse;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingQueryService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 运行时模型查询适配器。
 * <p>
 * 执行流程：
 * 1. 读取当前 LLM 运行时已注册的 provider；
 * 2. 遍历每个 provider 的可用模型；
 * 3. 汇总为前端可展示的模型清单。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class HarnessRuntimeModelQueryApi {

    private final IModelSettingQueryService modelSettingQueryService;

    public HarnessRuntimeModelQueryApi(IModelSettingQueryService modelSettingQueryService) {
        this.modelSettingQueryService = modelSettingQueryService;
    }

    /**
     * 查询当前运行时可用模型。
     * <p>
     * 执行流程：
     * 1. 读取模型设置中已启用的模型作为基础清单；
     * 2. 拉取 provider 列表并逐个查询运行时模型；
     * 3. 按 provider + model 去重后合并返回。
     */
    public List<AvailableModelResponse> queryAll() {
        Map<String, AvailableModelResponse> merged = new LinkedHashMap<>();

        modelSettingQueryService.queryAll().stream()
                .filter(setting -> setting.enabled())
                .filter(setting -> setting.modelCode() != null && !setting.modelCode().isBlank())
                .forEach(setting -> {
                    String key = setting.providerCode() + ":" + setting.modelCode();
                    merged.putIfAbsent(key, new AvailableModelResponse(
                            setting.providerCode(),
                            setting.modelCode(),
                            setting.modelCode()
                    ));
                });

        return List.copyOf(merged.values());
    }
}
