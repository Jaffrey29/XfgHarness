package cn.xiaofuge.deepseek.harness.trigger.http.query;

import cn.xiaofuge.deepseek.harness.api.response.conversation.ConversationMessageResponse;
import cn.xiaofuge.deepseek.harness.api.response.conversation.ConversationSessionSummaryResponse;
import cn.xiaofuge.deepseek.harness.api.service.query.HarnessConsoleQueryApi;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面向 Web 控制台暴露会话查询端点。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@RestController
@RequestMapping("/api/harness/console")
public class HarnessConsoleQueryController {

    private final HarnessConsoleQueryApi harnessConsoleQueryApi;

    public HarnessConsoleQueryController(HarnessConsoleQueryApi harnessConsoleQueryApi) {
        this.harnessConsoleQueryApi = harnessConsoleQueryApi;
    }

    @GetMapping("/sessions")
    public List<ConversationSessionSummaryResponse> listRecentSessions(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        return harnessConsoleQueryApi.listRecentSessions(limit);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public List<ConversationMessageResponse> listMessages(@PathVariable("sessionId") String sessionId) {
        return harnessConsoleQueryApi.listMessages(sessionId);
    }
}
