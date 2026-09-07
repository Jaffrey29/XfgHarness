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
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class HarnessRuntimeModelQueryApi {

    private final IModelSettingQueryService modelSettingQueryService;

    /**
     * 处理Harness 运行时 模型 Query API。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @param modelSettingQueryService 模型 Setting Query Service参数。
     * @return 方法执行结果。
     */
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
