package cn.xiaofuge.deepseek.harness.trigger.http.query;

import cn.xiaofuge.deepseek.harness.api.response.workspace.WorkspaceEntryResponse;
import cn.xiaofuge.deepseek.harness.api.service.query.WorkspaceQueryApi;
import java.io.IOException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作区查询控制器。
 * <p>
 * 执行流程：
 * 1. 接收前端工作区查询请求；
 * 2. 调用工作区查询适配器读取目录与排序信息；
 * 3. 返回给前端用于下拉选择和目录浏览。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@RestController
@RequestMapping("/api/agent")
public class WorkspaceQueryController {

    private final WorkspaceQueryApi workspaceQueryApi;

    public WorkspaceQueryController(WorkspaceQueryApi workspaceQueryApi) {
        this.workspaceQueryApi = workspaceQueryApi;
    }

    /**
     * 获取全部工作区。
     * <p>
     * 执行流程：
     * 1. 调用查询适配器读取工作区列表；
     * 2. 保持响应结构不变；
     * 3. 返回给前端工作区页面。
     */
    @GetMapping("/workspaces")
    public List<WorkspaceEntryResponse> getWorkspaces() throws IOException {
        return workspaceQueryApi.getWorkspaces();
    }

    /**
     * 列出指定路径下的可见目录。
     * <p>
     * 执行流程：
     * 1. 读取可选的 path 参数；
     * 2. 调用查询适配器扫描目录；
     * 3. 返回目录选择器可直接使用的字符串列表。
     */
    @GetMapping("/workspaces/list")
    public List<String> listDirectories(@RequestParam(value = "path", required = false) String path) throws IOException {
        return workspaceQueryApi.listDirectories(path);
    }
}
