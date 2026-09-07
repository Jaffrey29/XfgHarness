package cn.xiaofuge.deepseek.harness.api.service.command;

import cn.xiaofuge.deepseek.harness.api.response.approval.ApprovalActionResponse;
import cn.xiaofuge.deepseek.harness.domain.task.IApprovalCommandService;
import org.springframework.stereotype.Service;

/**
 * 审批中心命令适配器。
 * <p>
 * 执行流程：
 * 1. 接收审批动作请求；
 * 2. 调用领域命令服务推进会话状态；
 * 3. 将执行结果包装成响应对象。
 *
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class HarnessApprovalCommandApi {

    private final IApprovalCommandService approvalCommandService;

    /**
     * 处理Harness 审批 命令 API。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @param approvalCommandService 审批 命令 Service参数。
     * @return 方法执行结果。
     */
    public HarnessApprovalCommandApi(IApprovalCommandService approvalCommandService) {
        this.approvalCommandService = approvalCommandService;
    }

    /**
     * 执行审批通过操作。
     * <p>
     * 执行流程：
     * 1. 传入 sessionId；
     * 2. 调用领域命令服务完成审批与续跑；
     * 3. 将结果转为 `ApprovalActionResponse` 返回。
     */
    public ApprovalActionResponse approve(String sessionId) {
        var result = approvalCommandService.approve(sessionId);
        return new ApprovalActionResponse(result.sessionId(), result.status(), result.message(), result.operatedAt());
    }
}
