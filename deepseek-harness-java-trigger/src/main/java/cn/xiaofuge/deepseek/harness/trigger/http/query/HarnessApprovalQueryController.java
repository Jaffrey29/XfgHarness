package cn.xiaofuge.deepseek.harness.trigger.http.query;

import cn.xiaofuge.deepseek.harness.api.response.approval.PendingApprovalResponse;
import cn.xiaofuge.deepseek.harness.api.service.query.HarnessApprovalQueryApi;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审批中心查询控制器。
 * <p>
 * 执行流程：
 * 1. 接收前端 GET 请求；
 * 2. 调用查询适配器读取待审批列表；
 * 3. 直接返回响应体。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@RestController
@RequestMapping("/api/harness/approvals")
public class HarnessApprovalQueryController {

    private final HarnessApprovalQueryApi harnessApprovalQueryApi;

    /**
     * 处理Harness Approval Query Controller相关逻辑。
     */
    public HarnessApprovalQueryController(HarnessApprovalQueryApi harnessApprovalQueryApi) {
        this.harnessApprovalQueryApi = harnessApprovalQueryApi;
    }

    /**
     * 查询待审批任务。
     * <p>
     * 执行流程：
     * 1. 读取 limit 参数；
     * 2. 调用查询适配器获取审批列表；
     * 3. 返回给前端。
     */
    @GetMapping("/pending")
    public List<PendingApprovalResponse> listPending(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        return harnessApprovalQueryApi.listPendingApprovals(limit);
    }
}
