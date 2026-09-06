package cn.walioffice.deepseek.harness.trigger.http.command;

import cn.walioffice.deepseek.harness.api.response.approval.ApprovalActionResponse;
import cn.walioffice.deepseek.harness.api.service.command.HarnessApprovalCommandApi;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审批中心命令控制器。
 * <p>
 * 执行流程：
 * 1. 接收前端审批动作请求；
 * 2. 调用命令适配器推进会话状态；
 * 3. 返回执行结果。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@RestController
@RequestMapping("/api/harness/approvals")
public class HarnessApprovalCommandController {

    private final HarnessApprovalCommandApi harnessApprovalCommandApi;

    public HarnessApprovalCommandController(HarnessApprovalCommandApi harnessApprovalCommandApi) {
        this.harnessApprovalCommandApi = harnessApprovalCommandApi;
    }

    /**
     * 执行审批通过。
     * <p>
     * 执行流程：
     * 1. 接收 sessionId；
     * 2. 交给命令适配器执行审批；
     * 3. 返回审批结果。
     */
    @PostMapping("/{sessionId}/approve")
    public ApprovalActionResponse approve(@PathVariable("sessionId") String sessionId) {
        return harnessApprovalCommandApi.approve(sessionId);
    }
}
