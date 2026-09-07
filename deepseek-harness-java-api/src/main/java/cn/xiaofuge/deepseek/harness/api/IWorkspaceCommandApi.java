package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.CreateWorkspaceRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.RenameWorkspaceRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ReorderWorkspaceRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.WorkspaceEntryResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import java.util.List;

public interface IWorkspaceCommandApi {

    Response<List<WorkspaceEntryResponseDTO>> createWorkspace(CreateWorkspaceRequestDTO request);

    Response<List<WorkspaceEntryResponseDTO>> deleteWorkspace(String name);

    Response<List<WorkspaceEntryResponseDTO>> renameWorkspace(String name, RenameWorkspaceRequestDTO request);

    Response<List<WorkspaceEntryResponseDTO>> reorderWorkspaces(ReorderWorkspaceRequestDTO request);
}
