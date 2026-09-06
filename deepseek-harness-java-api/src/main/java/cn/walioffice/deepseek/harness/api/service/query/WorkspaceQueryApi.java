package cn.walioffice.deepseek.harness.api.service.query;

import cn.walioffice.deepseek.harness.api.response.workspace.WorkspaceEntryResponse;
import cn.walioffice.deepseek.harness.api.service.workspace.WorkspaceRegistryService;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 工作区查询适配器。
 * <p>
 * 执行流程：
 * 1. 调用工作区注册服务读取当前目录与顺序信息；
 * 2. 组装为 `WorkspaceEntryResponse`；
 * 3. 返回给控制器展示。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class WorkspaceQueryApi {

    private final WorkspaceRegistryService workspaceRegistryService;

    public WorkspaceQueryApi(WorkspaceRegistryService workspaceRegistryService) {
        this.workspaceRegistryService = workspaceRegistryService;
    }

    /**
     * 查询当前所有工作区。
     * <p>
     * 执行流程：
     * 1. 从工作区注册服务读取配置和磁盘目录；
     * 2. 合并为排序后的列表；
     * 3. 返回给前端。
     */
    public List<WorkspaceEntryResponse> getWorkspaces() throws IOException {
        return workspaceRegistryService.listWorkspaces();
    }

    /**
     * 查询目录下可选子目录。
     * <p>
     * 执行流程：
     * 1. 接收 path 参数；
     * 2. 由工作区注册服务读取目录内容；
     * 3. 返回可供选择的目录列表。
     */
    public List<String> listDirectories(String path) throws IOException {
        return workspaceRegistryService.listDirectories(path);
    }
}
