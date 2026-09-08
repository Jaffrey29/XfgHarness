package cn.xiaofuge.deepseek.harness.trigger.http.query;

import cn.xiaofuge.deepseek.harness.api.IHarnessConfigQueryApi;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the effective harness configuration for diagnostics.
 */
@RestController
@RequestMapping("/api/harness/config")
public class HarnessConfigQueryController {

    private final IHarnessConfigQueryApi configQueryApi;

    public HarnessConfigQueryController(IHarnessConfigQueryApi configQueryApi) {
        this.configQueryApi = configQueryApi;
    }

    @GetMapping("/effective")
    public Response<Map<String, Object>> effectiveConfig() {
        return configQueryApi.effectiveConfig();
    }
}
