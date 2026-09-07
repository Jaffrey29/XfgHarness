package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.WorkspaceEntryResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import java.util.List;

public interface IWorkspaceQueryApi {

    Response<List<WorkspaceEntryResponseDTO>> getWorkspaces();

    Response<List<String>> listDirectories(String path);
}
