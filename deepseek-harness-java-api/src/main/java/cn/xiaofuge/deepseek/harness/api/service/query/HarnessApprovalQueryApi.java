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
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class HarnessApprovalQueryApi {

    private final IApprovalQueryService approvalQueryService;

    /**
     * 处理Harness 审批 Query API。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @param approvalQueryService 审批 Query Service参数。
     * @return 方法执行结果。
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
