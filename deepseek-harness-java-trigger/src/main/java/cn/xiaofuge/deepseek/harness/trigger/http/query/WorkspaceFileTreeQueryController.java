package cn.xiaofuge.deepseek.harness.trigger.http.query;

import cn.xiaofuge.deepseek.harness.trigger.service.workspace.WorkspaceRegistryService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作区文件树查询端点（Web 端「信息」面板的工程文件浏览）。
 * <p>
 * 背景：桌面端信息面板直接读本机目录渲染文件树；Web 端浏览器与服务器不同机，
 * 需要一个受控的递归目录列举接口。
 * <p>
 * 安全边界：起始路径必须落在 {@code harness.workspace.root} 内
 * （见 {@link WorkspaceRegistryService#fileTree(String, int)}），越界一律 400。
 * <p>调用流程：Controller → WorkspaceRegistryService 校验+递归 → 返回嵌套节点树。</p>
 */
@RestController
@RequestMapping("/api/agent/workspace")
public class WorkspaceFileTreeQueryController {

    private final WorkspaceRegistryService workspaceRegistryService;

    public WorkspaceFileTreeQueryController(WorkspaceRegistryService workspaceRegistryService) {
        this.workspaceRegistryService = workspaceRegistryService;
    }

    /**
     * 递归列出目录文件树。
     *
     * @param path 起始目录（空 = 工作区根目录），必须在根目录内
     * @param depth 最大递归深度（默认 6，1-12 之间）
     * @return 嵌套节点树：{name,path,type,size,children[]}
     */
    @GetMapping("/tree")
    public ResponseEntity<?> tree(
            @RequestParam(value = "path", required = false) String path,
            @RequestParam(value = "depth", required = false, defaultValue = "6") int depth
    ) {
        int safeDepth = Math.min(12, Math.max(1, depth));
        try {
            Map<String, Object> tree = workspaceRegistryService.fileTree(path, safeDepth);
            return ResponseEntity.ok(tree);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "A0001", "info", exception.getMessage()));
        }
    }
}
