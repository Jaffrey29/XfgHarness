package cn.walioffice.deepseek.harness.domain.runtime.tool.model.valobj;

/**
 * Catalog entry for one tool in the tool registry.
 *
 * @param toolCode tool identifier
 * @param displayName human-readable name
 * @param description tool description
 * @param category tool category (e.g. "fs", "shell", "web", "mcp")
 * @param enabled whether the tool is currently enabled
 * @param requiresApproval whether this tool requires approval before execution
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ToolCatalogEntryVO(
        String toolCode,
        String displayName,
        String description,
        String category,
        boolean enabled,
        boolean requiresApproval
) {
}
