package cn.xiaofuge.deepseek.harness.cases.agent.node;

import cn.xiaofuge.deepseek.harness.api.dto.agent.AgentMessageRequest;
import cn.xiaofuge.deepseek.harness.api.dto.agent.AgentMessageResponse;
import cn.xiaofuge.deepseek.harness.cases.agent.factory.AgentMessageDynamicContext;
import cn.xiaofuge.deepseek.harness.domain.agent.model.valobj.InboxTarget;
import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;
import cn.xiaofuge.deepseek.harness.domain.model.entity.MessageSource;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ContentBlock;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ImageAttachmentRef;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ImageBlock;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;
import cn.xiaofuge.deepseek.harness.domain.shared.StrategyHandler;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 派发节点：构造用户消息并发送到 Agent Inbox。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service("agentMessageDispatchNode")
public class AgentDispatchNode extends AbstractAgentMessageNode {

    private static final Logger log = LoggerFactory.getLogger(AgentDispatchNode.class);

    // 提取消息中的绝对路径：遇到空白和常见中英文标点即停止，避免把后续说明文字吞进路径。
    private static final Pattern ABSOLUTE_PATH = Pattern.compile("(/[^\s，。,；;：:\"'“”‘’（）()！？!?、<>]+)");
    private static final Pattern DIRECTORY_QUESTION = Pattern.compile("(?i)(目录|项目|文件夹|仓库|里面有什么|有哪些|包含什么|列表|list|what's in|what is in)");

    @Autowired
    @Qualifier("agentMessageCollectNode")
    private AgentCollectNode collectNode;

    @Override
    protected AgentMessageResponse doApply(AgentMessageRequest request,
                                           AgentMessageDynamicContext ctx) throws Exception {
        var intent = ctx.getIntent();
        String messageText = prepareMessageText(request.message(), intent);
        List<ContentBlock> blocks = new java.util.ArrayList<>();
        blocks.add(new TextBlock(messageText));
        appendImageBlocks(request, blocks);
        Message userMessage = Message.createUser(
                blocks,
                new MessageSource.UserSource()
        );
        ctx.setUserMessage(userMessage);
        log.info("[Dispatch] Agent={} 构造用户消息并发送到 inbox（intent={} blocks={} 文本长度={}）",
                request.agentId(), intent, blocks.size(), messageText.length());
        ctx.getAgent().send(userMessage, InboxTarget.NEXT_TURN, true);
        log.info("[Dispatch] Agent={} 消息已投递 inbox 并唤醒 Agent，等待执行...", request.agentId());
        return null;
    }

    /**
     * 把请求携带的图片附件解析为 ImageBlock 追加到内容块。
     * 支持两种形式：data URL（data:image/png;base64,xxx）或裸 base64（需配 mimeType）。
     * 解析失败的附件跳过并记录（不中断消息发送）。
     */
    private static void appendImageBlocks(AgentMessageRequest request, List<ContentBlock> blocks) {
        if (request.images() == null || request.images().isEmpty()) {
            return;
        }
        for (String raw : request.images()) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                String mime = "image/png";
                String base64 = raw.trim();
                // data URL 形式: data:<mime>;base64,<payload>
                if (base64.startsWith("data:")) {
                    int comma = base64.indexOf(',');
                    if (comma < 0) {
                        continue;
                    }
                    String header = base64.substring(5, comma); // strip "data:"
                    int semi = header.indexOf(';');
                    mime = semi > 0 ? header.substring(0, semi) : header;
                    if (mime == null || mime.isBlank()) {
                        mime = "image/png";
                    }
                    base64 = base64.substring(comma + 1);
                }
                byte[] data = java.util.Base64.getDecoder().decode(base64);
                if (data.length == 0) {
                    continue;
                }
                blocks.add(new ImageBlock(new ImageAttachmentRef(data, mime, 0, 0)));
            } catch (IllegalArgumentException e) {
                // 非法 base64：跳过该附件，不让整条消息失败
                log.warn("[Dispatch] 图片附件解析失败，已跳过（原始长度={}）", raw.length());
            }
        }
    }

    @Override
    public StrategyHandler<AgentMessageRequest, AgentMessageDynamicContext, AgentMessageResponse>
    getNext(AgentMessageRequest request, AgentMessageDynamicContext ctx) {
        return collectNode;
    }

    static String prepareMessageText(String messageText, AgentMessageDynamicContext.MessageIntent intent) {
        if (intent == AgentMessageDynamicContext.MessageIntent.TASK_EXECUTION) {
            String directoryInstruction = buildDirectoryInstruction(messageText);
            if (directoryInstruction != null) {
                return directoryInstruction;
            }
            return "请先使用可用工具完成下面的任务，不要在未使用工具前直接给出结论。"
                    + "如果任务涉及本地目录、文件、代码或仓库状态，优先读取/搜索后再回答。\n\n"
                    + "用户原始请求：" + messageText;
        }
        return messageText;
    }

    private static String buildDirectoryInstruction(String messageText) {
        if (messageText == null || !DIRECTORY_QUESTION.matcher(messageText).find()) {
            return null;
        }
        Matcher matcher = ABSOLUTE_PATH.matcher(messageText);
        if (!matcher.find()) {
            return null;
        }
        String directory = matcher.group(1);
        return "你需要列出这个目录的一级子目录和文件。推荐用 `shell_execute` 工具执行 `ls -la \"" + directory + "\"` 获取完整列表，\n"
                + "或者用 `find \"" + directory + "\" -maxdepth 1 -type d` 只列目录。\n"
                + "拿到真实结果后，用 Markdown 列表展示所有一级项目，不要遗漏、不要模糊化。\n\n"
                + "用户原始请求：" + messageText;
    }
}
