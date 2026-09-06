package cn.walioffice.deepseek.harness.trigger.http.command;

import cn.walioffice.deepseek.harness.api.command.settings.DiscoverModelsRequest;
import cn.walioffice.deepseek.harness.api.service.command.HarnessModelDiscoveryApi;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 面向 Web 控制台代理上游模型发现。 */
@RestController
@RequestMapping("/api/harness/settings/models/discover")
public class HarnessModelDiscoveryController {

    private final HarnessModelDiscoveryApi harnessModelDiscoveryApi;

    public HarnessModelDiscoveryController(HarnessModelDiscoveryApi harnessModelDiscoveryApi) {
        this.harnessModelDiscoveryApi = harnessModelDiscoveryApi;
    }

    @PostMapping
    public ResponseEntity<?> discover(@RequestBody DiscoverModelsRequest request) {
        try {
            List<String> models = harnessModelDiscoveryApi.discover(request);
            return ResponseEntity.ok(models);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(java.util.Map.of("error", e.getMessage() != null ? e.getMessage() : "发现模型失败"));
        }
    }
}
