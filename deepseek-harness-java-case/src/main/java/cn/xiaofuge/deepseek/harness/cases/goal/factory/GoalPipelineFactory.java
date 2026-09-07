package cn.xiaofuge.deepseek.harness.cases.goal.factory;

import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalCommandVO;
import cn.xiaofuge.deepseek.harness.api.dto.GoalViewResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.goal.node.GoalCommandNode;
import cn.xiaofuge.deepseek.harness.cases.goal.node.GoalQueryNode;
import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipeline;
import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipelineFactory;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import org.springframework.stereotype.Service;

/**
 * 目标查询与命令 Pipeline 工厂。
 * <p>流程：查询 → GoalQueryNode；命令 → GoalCommandNode；统一返回目标视图 DTO。</p>
 * <p>数据示例：查询输入 {@code session-001}；命令输入包含
 * {@code sessionId=session-001, action=CREATE}。</p>
 */
@Service
public class GoalPipelineFactory implements CasePipelineFactory<String, NoContext, GoalViewResponseDTO> {

    private final GoalQueryNode goalQueryNode;
    private final GoalCommandNode goalCommandNode;

    public GoalPipelineFactory(GoalQueryNode goalQueryNode, GoalCommandNode goalCommandNode) {
        this.goalQueryNode = goalQueryNode;
        this.goalCommandNode = goalCommandNode;
    }

    /**
     * 构建目标查询 Pipeline。
     */
    public CasePipeline<String, NoContext, GoalViewResponseDTO> queryPipeline() {
        return CasePipeline.of("Goal query", goalQueryNode, () -> NoContext.INSTANCE);
    }

    /**
     * 构建目标命令执行 Pipeline。
     */
    public CasePipeline<GoalCommandNode.Input, NoContext, GoalViewResponseDTO> commandPipeline() {
        return CasePipeline.of("Goal command", goalCommandNode, () -> NoContext.INSTANCE);
    }

    /**
     * 构建默认执行 Pipeline，绑定首节点和上下文。
     */
    public CasePipeline<String, NoContext, GoalViewResponseDTO> pipeline() {
        return queryPipeline();
    }
}
