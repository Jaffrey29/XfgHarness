package cn.xiaofuge.deepseek.harness.cases.session;

import cn.xiaofuge.deepseek.harness.api.dto.ConversationMessageResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ConversationSessionSummaryResponseDTO;

import java.util.List;

public interface IConversationQueryCase {
    List<ConversationSessionSummaryResponseDTO> listRecentSessions(int limit);
    List<ConversationMessageResponseDTO> listMessages(String sessionId);
}
