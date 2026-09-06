package cn.xiaofuge.deepseek.harness.domain.acp.model.valobj;

import java.util.List;

/**
 * One ACP message in a session turn.
 *
 * @param role     message role (user, assistant)
 * @param content  list of content blocks (text, image)
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record AcpMessage(String role, List<AcpContentBlock> content) {

    public sealed interface AcpContentBlock permits AcpTextBlock, AcpImageBlock {}

    public record AcpTextBlock(String text) implements AcpContentBlock {}

    public record AcpImageBlock(String mediaType, String base64Data) implements AcpContentBlock {}
}
