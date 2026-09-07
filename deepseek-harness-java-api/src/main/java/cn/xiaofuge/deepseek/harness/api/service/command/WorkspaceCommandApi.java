package cn.xiaofuge.deepseek.harness.api.service.command;

import cn.xiaofuge.deepseek.harness.api.dto.workspace.CreateWorkspaceRequest;
import cn.xiaofuge.deepseek.harness.api.dto.workspace.RenameWorkspaceRequest;
import cn.xiaofuge.deepseek.harness.api.dto.workspace.ReorderWorkspaceRequest;
import cn.xiaofuge.deepseek.harness.api.response.workspace.WorkspaceEntryResponse;
import cn.xiaofuge.deepseek.harness.api.service.workspace.WorkspaceRegistryService;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 工作区命令适配器。
 * <p>
 * 执行流程：
 * 1. 接收工作区新增、删除、重命名、排序请求；
 * 2. 调用工作区注册服务修改磁盘状态；
 * 3. 返回最新工作区列表给前端。
 *
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class WorkspaceCommandApi {

    private final WorkspaceRegistryService workspaceRegistryService;

    /**
     * 处理工作区 命令 API。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @param workspaceRegistryService 工作区 Registry Service参数。
     * @return 方法执行结果。
     */
    public WorkspaceCommandApi(WorkspaceRegistryService workspaceRegistryService) {
        this.workspaceRegistryService = workspaceRegistryService;
    }

    /**
     * 创建工作区。
     * <p>
     * 执行流程：
     * 1. 校验工作区名称；
     * 2. 由工作区注册服务创建目录；
     * 3. 返回更新后的工作区列表。
     */
    public List<WorkspaceEntryResponse> createWorkspace(CreateWorkspaceRequest request) throws IOException {
        return workspaceRegistryService.createWorkspace(request);
    }

    /**
     * 删除工作区。
     * <p>
     * 执行流程：
     * 1. 传入工作区名称；
     * 2. 由工作区注册服务删除目录和顺序记录；
     * 3. 返回更新后的列表。
     */
    public List<WorkspaceEntryResponse> deleteWorkspace(String name) throws IOException {
        return workspaceRegistryService.deleteWorkspace(name);
    }

    /**
     * 重命名工作区。
     * <p>
     * 执行流程：
     * 1. 接收旧名称和新名称；
     * 2. 由工作区注册服务移动目录；
     * 3. 返回更新后的工作区列表。
     */
    public List<WorkspaceEntryResponse> renameWorkspace(String name, RenameWorkspaceRequest request) throws IOException {
        return workspaceRegistryService.renameWorkspace(name, request);
    }

    /**
     * 调整工作区排序。
     * <p>
     * 执行流程：
     * 1. 读取前端传入的顺序；
     * 2. 持久化顺序文件；
     * 3. 返回更新后的工作区列表。
     */
    public List<WorkspaceEntryResponse> reorderWorkspaces(ReorderWorkspaceRequest request) throws IOException {
        return workspaceRegistryService.reorderWorkspaces(request);
    }
}
