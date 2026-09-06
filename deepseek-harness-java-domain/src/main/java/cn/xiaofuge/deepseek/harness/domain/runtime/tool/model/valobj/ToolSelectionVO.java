package cn.xiaofuge.deepseek.harness.domain.runtime.tool.model.valobj;

import java.util.List;

/**
 * Holds the requested tool codes for one task submission.
 *
 * @param toolCodes requested tool codes
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ToolSelectionVO(List<String> toolCodes) {

    public ToolSelectionVO {
        toolCodes = toolCodes == null ? List.of() : List.copyOf(toolCodes);
    }
}
