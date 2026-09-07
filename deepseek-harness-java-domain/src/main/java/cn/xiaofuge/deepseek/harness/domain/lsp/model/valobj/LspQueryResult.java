package cn.xiaofuge.deepseek.harness.domain.lsp.model.valobj;

import java.util.List;
import java.util.Optional;

/**
 * Result of one LSP query.
 *
 * @param locations  locations (for definition/references/implementation)
 * @param hoverText  hover text (for hover queries)
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record LspQueryResult(List<LspLocation> locations, Optional<String> hoverText) {
    /**
     * 处理of Locations相关逻辑。
     */
    public static LspQueryResult ofLocations(List<LspLocation> locations) {
        return new LspQueryResult(locations, Optional.empty());
    }
    /**
     * 处理of Hover相关逻辑。
     */
    public static LspQueryResult ofHover(String text) {
        return new LspQueryResult(List.of(), Optional.of(text));
    }
}
