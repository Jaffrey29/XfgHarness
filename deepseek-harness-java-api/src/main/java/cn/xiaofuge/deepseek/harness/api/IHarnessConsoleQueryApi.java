package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.ConversationMessageResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ConversationSessionSummaryResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import java.util.List;

public interface IHarnessConsoleQueryApi {

    Response<List<ConversationSessionSummaryResponseDTO>> listRecentSessions(int limit);

    Response<List<ConversationMessageResponseDTO>> listMessages(String sessionId);
}
