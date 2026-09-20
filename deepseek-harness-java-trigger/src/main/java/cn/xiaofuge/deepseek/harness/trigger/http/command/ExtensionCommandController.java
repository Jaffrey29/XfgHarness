package cn.xiaofuge.deepseek.harness.trigger.http.command;

import cn.xiaofuge.deepseek.harness.api.response.Response;
import cn.xiaofuge.deepseek.harness.domain.extension.IExtensionManagementPort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 扩展管理命令接口：技能安装/启停/删除、MCP server 增删改与测试、CLI 配置保存。
 * <p>所有变更即时生效（MCP 重连注册工具、技能启停热更新、CLI 命令热替换）。</p>
 */
@RestController
@RequestMapping("/api/harness/extensions")
public class ExtensionCommandController {

    private final IExtensionManagementPort extensionManagement;

    public ExtensionCommandController(IExtensionManagementPort extensionManagement) {
        this.extensionManagement = extensionManagement;
    }

    // ── Skills ───────────────────────────────────────────

    @PostMapping("/skills/install")
    public Response<IExtensionManagementPort.ExtensionSkillInstallResult> installSkill(
            @RequestBody Map<String, String> body) {
        try {
            return Response.success(extensionManagement.installSkillFromGit(
                    body.get("gitUrl"), body.get("subdir"), body.get("name")));
        } catch (IllegalArgumentException e) {
            return Response.invalidArgument(e.getMessage());
        } catch (Exception e) {
            return Response.internalError(e.getMessage());
        }
    }

    @PostMapping("/skills/{name}/enabled")
    public Response<Boolean> setSkillEnabled(@PathVariable("name") String name,
                                             @RequestBody Map<String, Object> body) {
        try {
            boolean enabled = Boolean.TRUE.equals(body.get("enabled"))
                    || "true".equalsIgnoreCase(String.valueOf(body.get("enabled")));
            extensionManagement.setSkillEnabled(name, enabled);
            return Response.success(enabled);
        } catch (IllegalArgumentException e) {
            return Response.invalidArgument(e.getMessage());
        } catch (Exception e) {
            return Response.internalError(e.getMessage());
        }
    }

    @DeleteMapping("/skills/{name}")
    public Response<Boolean> removeSkill(@PathVariable("name") String name,
                                         @RequestParam(value = "cwd", required = false) String cwd) {
        try {
            extensionManagement.removeSkill(name, cwd);
            return Response.success(Boolean.TRUE);
        } catch (IllegalArgumentException e) {
            return Response.invalidArgument(e.getMessage());
        } catch (Exception e) {
            return Response.internalError(e.getMessage());
        }
    }

    // ── MCP ──────────────────────────────────────────────

    @PutMapping("/mcp/servers")
    public Response<IExtensionManagementPort.ExtensionMcpApplyResult> upsertMcpServer(
            @RequestBody IExtensionManagementPort.ExtensionMcpServer server) {
        try {
            return Response.success(extensionManagement.upsertMcpServer(server));
        } catch (IllegalArgumentException e) {
            return Response.invalidArgument(e.getMessage());
        } catch (Exception e) {
            return Response.internalError(e.getMessage());
        }
    }

    @PostMapping("/mcp/servers/test")
    public Response<IExtensionManagementPort.ExtensionMcpApplyResult> testMcpServer(
            @RequestBody IExtensionManagementPort.ExtensionMcpServer server) {
        try {
            return Response.success(extensionManagement.testMcpServer(server));
        } catch (IllegalArgumentException e) {
            return Response.invalidArgument(e.getMessage());
        } catch (Exception e) {
            return Response.internalError(e.getMessage());
        }
    }

    @DeleteMapping("/mcp/servers/{name}")
    public Response<Boolean> removeMcpServer(@PathVariable("name") String name) {
        try {
            extensionManagement.removeMcpServer(name);
            return Response.success(Boolean.TRUE);
        } catch (IllegalArgumentException e) {
            return Response.invalidArgument(e.getMessage());
        } catch (Exception e) {
            return Response.internalError(e.getMessage());
        }
    }

    // ── CLI ──────────────────────────────────────────────

    @PutMapping("/cli")
    public Response<IExtensionManagementPort.ExtensionCliConfig> setCli(
            @RequestBody IExtensionManagementPort.ExtensionCliConfig config) {
        try {
            return Response.success(extensionManagement.setCliConfig(config));
        } catch (IllegalArgumentException e) {
            return Response.invalidArgument(e.getMessage());
        } catch (Exception e) {
            return Response.internalError(e.getMessage());
        }
    }
}
