package cn.xiaofuge.deepseek.harness.trigger.http.command;

import cn.xiaofuge.deepseek.harness.api.dto.workspace.CreateWorkspaceRequest;
import cn.xiaofuge.deepseek.harness.api.dto.workspace.RenameWorkspaceRequest;
import cn.xiaofuge.deepseek.harness.api.dto.workspace.ReorderWorkspaceRequest;
import cn.xiaofuge.deepseek.harness.api.response.workspace.WorkspaceEntryResponse;
import cn.xiaofuge.deepseek.harness.api.service.command.WorkspaceCommandApi;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 面向 Agent UI 暴露工作区写操作。 */
@RestController
@RequestMapping("/api/agent")
public class WorkspaceCommandController {

    private final WorkspaceCommandApi workspaceCommandApi;

    public WorkspaceCommandController(WorkspaceCommandApi workspaceCommandApi) {
        this.workspaceCommandApi = workspaceCommandApi;
    }

    @PostMapping("/workspaces")
    public List<WorkspaceEntryResponse> createWorkspace(@RequestBody CreateWorkspaceRequest request) throws IOException {
        return workspaceCommandApi.createWorkspace(request);
    }

    @DeleteMapping("/workspaces/{name}")
    public List<WorkspaceEntryResponse> deleteWorkspace(@PathVariable("name") String name) throws IOException {
        return workspaceCommandApi.deleteWorkspace(name);
    }

    @PatchMapping("/workspaces/{name}")
    public List<WorkspaceEntryResponse> renameWorkspace(@PathVariable("name") String name,
                                                        @RequestBody RenameWorkspaceRequest request) throws IOException {
        return workspaceCommandApi.renameWorkspace(name, request);
    }

    @PutMapping("/workspaces/order")
    public List<WorkspaceEntryResponse> reorderWorkspaces(@RequestBody ReorderWorkspaceRequest request) throws IOException {
        return workspaceCommandApi.reorderWorkspaces(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleIllegalArgument() {
        // 返回 400，不输出响应体
    }
}
