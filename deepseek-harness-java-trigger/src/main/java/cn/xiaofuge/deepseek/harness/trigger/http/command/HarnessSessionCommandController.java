package cn.xiaofuge.deepseek.harness.trigger.http.command;

import cn.xiaofuge.deepseek.harness.api.dto.SessionRestoreResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import cn.xiaofuge.deepseek.harness.cases.session.ISessionRestoreCase;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/harness/sessions")
public class HarnessSessionCommandController {

    private final ISessionRestoreCase sessionRestoreCase;

    public HarnessSessionCommandController(ISessionRestoreCase sessionRestoreCase) {
        this.sessionRestoreCase = sessionRestoreCase;
    }

    @PostMapping("/{sessionId}/restore")
    public Response<SessionRestoreResponseDTO> restore(@PathVariable("sessionId") String sessionId) {
        return Response.success(sessionRestoreCase.execute(new cn.xiaofuge.deepseek.harness.api.dto.SessionRestoreRequestDTO(sessionId)));
    }
}
