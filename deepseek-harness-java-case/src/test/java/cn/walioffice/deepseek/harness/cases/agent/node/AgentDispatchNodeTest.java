package cn.walioffice.deepseek.harness.cases.agent.node;

import cn.walioffice.deepseek.harness.cases.agent.factory.AgentMessageDynamicContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentDispatchNodeTest {

    @Test
    void buildsFsSearchInstructionForDirectoryQuestion() {
        String message = AgentDispatchNode.prepareMessageText(
                "这个目录下有什么项目？/Users/fuzhengwei/coding/github",
                AgentMessageDynamicContext.MessageIntent.TASK_EXECUTION
        );

        assertTrue(message.contains("shell_execute"));
        assertTrue(message.contains("ls -la \"/Users/fuzhengwei/coding/github\""));
        assertTrue(message.contains("maxdepth 1"));
    }

    @Test
    void fallsBackToGeneralToolInstructionForOtherTaskExecution() {
        String message = AgentDispatchNode.prepareMessageText(
                "帮我检查这个仓库",
                AgentMessageDynamicContext.MessageIntent.TASK_EXECUTION
        );

        assertTrue(message.contains("请先使用可用工具完成下面的任务"));
    }
}
