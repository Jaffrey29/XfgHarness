package cn.xiaofuge.deepseek.harness.api.service.query;

import cn.xiaofuge.deepseek.harness.api.response.approval.PendingApprovalResponse;
import cn.xiaofuge.deepseek.harness.domain.task.IApprovalQueryService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 审批中心查询适配器。
 * <p>
 * 执行流程：
 * 1. 调用领域查询服务获取待审批会话；
 * 2. 将领域 VO 转为前端响应对象；
 * 3. 返回给控制器展示。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class HarnessApprovalQueryApi {

    private final IApprovalQueryService approvalQueryService;

    /**
     * 处理Harness Approval Query Api相关逻辑。
     */
    public HarnessApprovalQueryApi(IApprovalQueryService approvalQueryService) {
        this.approvalQueryService = approvalQueryService;
    }

    /**
     * 查询待审批列表。
     * <p>
     * 执行流程：
     * 1. 按 limit 获取待审批会话 VO；
     * 2. 映射为 `PendingApprovalResponse`；
     * 3. 返回给前端审批中心。
     */
    public List<PendingApprovalResponse> listPendingApprovals(int limit) {
        return approvalQueryService.listPendingApprovals(limit).stream()
                .map(item -> new PendingApprovalResponse(item.sessionId(), item.title(), item.preview(), item.status(), item.createdAt()))
                .toList();
    }
}
