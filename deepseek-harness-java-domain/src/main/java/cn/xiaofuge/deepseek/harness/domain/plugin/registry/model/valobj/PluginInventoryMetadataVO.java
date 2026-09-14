package cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj;

public record PluginInventoryMetadataVO(
        String manifestPath,
        String author,
        String description,
        String scope
) {
}
