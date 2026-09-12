package cn.xiaofuge.deepseek.harness.cases.runtime;

import cn.xiaofuge.deepseek.harness.api.dto.AvailableModelResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ModelSettingResponseDTO;
import java.util.List;

/**
 * 模型设置读操作用例契约。
 * <p>流程：Trigger/前端 → 本接口 → 查询 Pipeline → 模型设置领域查询服务 → DTO 列表。</p>
 * <p>数据示例：无入参；配置列表返回 {@code channelCode=default, baseUrl=http://127.0.0.1:8777/v1}；
 * 可用模型返回 {@code modelCode=glm-5.3-flash}。</p>
 */
public interface IModelSettingQueryCase {
    List<ModelSettingResponseDTO> listSettings();
    List<AvailableModelResponseDTO> listAvailableModels();
}
