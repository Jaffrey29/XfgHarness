package cn.walioffice.deepseek.harness.domain.runtime.tool.model.entity;

import java.util.Objects;

/**
 * Stores one persisted tool catalog entry.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class ToolCatalogEntryEntity {

    private final String toolCode;
    private final String displayName;
    private final String description;
    private final String category;
    private final boolean requiresApproval;

    public ToolCatalogEntryEntity(
            String toolCode,
            String displayName,
            String description,
            String category,
            boolean requiresApproval
    ) {
        this.toolCode = Objects.requireNonNull(toolCode, "toolCode");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.description = description;
        this.category = Objects.requireNonNull(category, "category");
        this.requiresApproval = requiresApproval;
    }

    public String getToolCode() { return toolCode; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public boolean isRequiresApproval() { return requiresApproval; }
}
