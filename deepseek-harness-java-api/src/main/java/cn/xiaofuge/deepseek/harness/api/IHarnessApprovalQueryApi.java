package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.PendingApprovalResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import java.util.List;

public interface IHarnessApprovalQueryApi {

    Response<List<PendingApprovalResponseDTO>> listPendingApprovals(int limit);
}
