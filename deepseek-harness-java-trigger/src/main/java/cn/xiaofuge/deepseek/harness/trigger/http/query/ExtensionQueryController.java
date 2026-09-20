package cn.xiaofuge.deepseek.harness.trigger.http.query;

import cn.xiaofuge.deepseek.harness.api.response.Response;
import cn.xiaofuge.deepseek.harness.domain.extension.IExtensionManagementPort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 扩展管理查询接口：技能列表、MCP server 列表、CLI 配置。
 */
@RestController
@RequestMapping("/api/harness/extensions")
public class ExtensionQueryController {

    private final IExtensionManagementPort extensionManagement;

    public ExtensionQueryController(IExtensionManagementPort extensionManagement) {
        this.extensionManagement = extensionManagement;
    }

    @GetMapping("/skills")
    public Response<List<IExtensionManagementPort.ExtensionSkillSummary>> skills(
            @RequestParam(value = "cwd", required = false) String cwd) {
        try {
            return Response.success(extensionManagement.listSkills(cwd));
        } catch (Exception e) {
            return Response.internalError(e.getMessage());
        }
    }

    @GetMapping("/mcp/servers")
    public Response<List<IExtensionManagementPort.ExtensionMcpServer>> mcpServers() {
        try {
            return Response.success(extensionManagement.listMcpServers());
        } catch (Exception e) {
            return Response.internalError(e.getMessage());
        }
    }

    @GetMapping("/cli")
    public Response<IExtensionManagementPort.ExtensionCliConfig> cli() {
        try {
            return Response.success(extensionManagement.getCliConfig());
        } catch (Exception e) {
            return Response.internalError(e.getMessage());
        }
    }
}
