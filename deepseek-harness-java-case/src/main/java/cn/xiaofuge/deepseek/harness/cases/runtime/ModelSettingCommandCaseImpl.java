package cn.xiaofuge.deepseek.harness.cases.runtime;

import cn.xiaofuge.deepseek.harness.api.dto.SaveModelSettingRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ModelSettingResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.runtime.factory.RuntimePipelineFactory;
import org.springframework.stereotype.Service;

/**
 * 模型设置写操作编排实现。
 * <p>流程：将保存/删除请求交给对应 Pipeline → 执行领域命令节点 → 保存返回 DTO，删除无返回。</p>
 * <p>数据示例：保存 {@code profileCode=default} 后返回 {@code enabled=true}；
 * 删除 {@code profileCode=default} 后配置从仓储移除。</p>
 */
@Service
public class ModelSettingCommandCaseImpl implements IModelSettingCommandCase {
    private final RuntimePipelineFactory runtimePipelineFactory;

    public ModelSettingCommandCaseImpl(RuntimePipelineFactory runtimePipelineFactory) {
        this.runtimePipelineFactory = runtimePipelineFactory;
    }

    /**
     * 编排保存操作，将命令交给对应 Pipeline 执行。
     */
    @Override
    public ModelSettingResponseDTO save(SaveModelSettingRequestDTO command) {
        return runtimePipelineFactory.savePipeline().execute(command);
    }

    /**
     * 编排删除操作，将目标标识交给对应 Pipeline 执行。
     */
    @Override
    public void delete(String profileCode) {
        runtimePipelineFactory.deletePipeline().execute(profileCode);
    }
}
