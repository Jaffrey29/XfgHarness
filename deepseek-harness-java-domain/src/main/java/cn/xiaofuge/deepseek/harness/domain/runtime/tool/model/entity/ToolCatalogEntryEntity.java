package cn.xiaofuge.deepseek.harness.domain.runtime.tool.model.entity;

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

    /**
     * 处理Tool Catalog Entry Entity相关逻辑。
     */
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

    /**
     * 获取Tool Code。
     */
    public String getToolCode() { return toolCode; }
    /**
     * 获取Display Name。
     */
    public String getDisplayName() { return displayName; }
    /**
     * 获取Description。
     */
    public String getDescription() { return description; }
    /**
     * 获取Category。
     */
    public String getCategory() { return category; }
    /**
     * 判断Requires Approval是否成立。
     */
    public boolean isRequiresApproval() { return requiresApproval; }
}
