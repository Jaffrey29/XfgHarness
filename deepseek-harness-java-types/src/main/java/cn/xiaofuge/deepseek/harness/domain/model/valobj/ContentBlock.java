package cn.xiaofuge.deepseek.harness.domain.model.valobj;

/**
 * 所有内容块类型的密封接口联合。
 * <p>
 * 对应 TS 侧 {@code ContentBlock}。在 Java 中使用 sealed 层级，保证 switch 分支可以穷尽处理。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public sealed interface ContentBlock permits
        TextBlock, ReasoningBlock, ImageBlock, ToolCallBlock, ToolResultBlock {
}
