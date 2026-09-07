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

    /**
     * 处理工作区 命令 Controller。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param workspaceCommandApi 工作区 命令 API参数。
     * @return 方法执行结果。
     */
    public WorkspaceCommandController(WorkspaceCommandApi workspaceCommandApi) {
        this.workspaceCommandApi = workspaceCommandApi;
    }

    /**
     * 创建 工作区。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param request 请求对象。
     * @return 创建的领域对象。
     */
    @PostMapping("/workspaces")
    public List<WorkspaceEntryResponse> createWorkspace(@RequestBody CreateWorkspaceRequest request) throws IOException {
        return workspaceCommandApi.createWorkspace(request);
    }

    /**
     * 删除 工作区。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param name 名称。
     * @return 方法执行结果。
     */
    @DeleteMapping("/workspaces/{name}")
    public List<WorkspaceEntryResponse> deleteWorkspace(@PathVariable("name") String name) throws IOException {
        return workspaceCommandApi.deleteWorkspace(name);
    }

    /**
     * 重命名工作区并刷新目录顺序。
     * <p>流程：读取路径参数和请求体 → 调用工作区命令服务 → 返回更新后的工作区列表。</p>
     *
     * @param name 原工作区名称。
     * @param request 新工作区名称。
     * @return 更新后的工作区列表。
     */
    @PatchMapping("/workspaces/{name}")
    public List<WorkspaceEntryResponse> renameWorkspace(@PathVariable("name") String name,
                                                        @RequestBody RenameWorkspaceRequest request) throws IOException {
        return workspaceCommandApi.renameWorkspace(name, request);
    }

    /**
     * 处理reorder Workspaces。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param request 请求对象。
     * @return 方法执行结果。
     */
    @PutMapping("/workspaces/order")
    public List<WorkspaceEntryResponse> reorderWorkspaces(@RequestBody ReorderWorkspaceRequest request) throws IOException {
        return workspaceCommandApi.reorderWorkspaces(request);
    }

    /**
     * 将非法参数异常转换为 400 响应。
     * <p>流程：捕获 IllegalArgumentException → 设置状态码 → 不输出响应体。</p>
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleIllegalArgument() {
        // 返回 400，不输出响应体
    }
}
