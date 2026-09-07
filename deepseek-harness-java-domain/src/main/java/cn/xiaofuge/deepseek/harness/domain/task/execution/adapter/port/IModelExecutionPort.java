package cn.xiaofuge.deepseek.harness.domain.task.execution.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.task.execution.model.valobj.ModelExecutionResultVO;

/**
 * 模型执行端口。
 * <p>
 * 执行流程：
 * 1. 领域层提交执行画像和提示词；
 * 2. 基础设施层调用具体模型提供方；
 * 3. 返回模型回复及相关元数据。
 *
 * <p>调用流程：提交 → 权限评估 → 审批 → 队列 → 执行 → 状态回写。</p>
 * <p>示例：需要审批的命令先挂起，批准后进入执行队列。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IModelExecutionPort {

    /**
     * 使用指定画像执行一次模型调用。
     * <p>
     * 执行流程：
     * 1. 读取 profileCode 和 prompt；
     * 2. 交给基础设施层执行实际模型请求；
     * 3. 返回助手回复和提供方信息。
     */
    ModelExecutionResultVO execute(String profileCode, String prompt);
}
