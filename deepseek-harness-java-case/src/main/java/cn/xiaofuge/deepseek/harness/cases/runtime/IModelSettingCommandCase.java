package cn.xiaofuge.deepseek.harness.cases.runtime;

import cn.xiaofuge.deepseek.harness.api.dto.SaveModelSettingRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ModelSettingResponseDTO;

/**
 * 模型设置写操作用例契约。
 * <p>流程：Trigger/前端 → 本接口 → 写 Pipeline → 模型设置领域命令服务。</p>
 * <p>数据示例：保存传入 {@code profileCode=default, modelCode=glm-5.3-flash}；
 * 删除传入 {@code profileCode=default} 后无返回值。</p>
 */
public interface IModelSettingCommandCase {
    ModelSettingResponseDTO save(SaveModelSettingRequestDTO command);
    void delete(String profileCode);
}
