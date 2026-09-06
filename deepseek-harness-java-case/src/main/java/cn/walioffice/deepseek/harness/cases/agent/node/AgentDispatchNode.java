package cn.walioffice.deepseek.harness.cases.agent.node;

import cn.walioffice.deepseek.harness.api.dto.agent.AgentMessageRequest;
import cn.walioffice.deepseek.harness.api.dto.agent.AgentMessageResponse;
import cn.walioffice.deepseek.harness.cases.agent.factory.AgentMessageDynamicContext;
import cn.walioffice.deepseek.harness.domain.agent.model.valobj.InboxTarget;
import cn.walioffice.deepseek.harness.domain.llm.model.entity.Message;
import cn.walioffice.deepseek.harness.domain.llm.model.entity.MessageSource;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ContentBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.TextBlock;
import cn.walioffice.deepseek.harness.domain.shared.strategy.StrategyHandler;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

    private static final Pattern ABSOLUTE_PATH = Pattern.compile("(/[^\s，。,；;]+)");
    private static final Pattern DIRECTORY_QUESTION = Pattern.compile("(?i)(目录|项目|文件夹|仓库|里面有什么|有哪些|包含什么|列表|list|what's in|what is in)");

    @Autowired
    @Qualifier("agentMessageCollectNode")
    private AgentCollectNode collectNode;

    @Override
    protected AgentMessageResponse doApply(AgentMessageRequest request,
                                           AgentMessageDynamicContext ctx) throws Exception {
        var intent = ctx.getIntent();
        String messageText = prepareMessageText(request.message(), intent);
        Message userMessage = Message.createUser(
                List.of(new TextBlock(messageText)),
                new MessageSource.UserSource()
        );
        ctx.setUserMessage(userMessage);
        ctx.getAgent().send(userMessage, InboxTarget.NEXT_TURN, true);
        return null;
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
