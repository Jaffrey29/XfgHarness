package cn.xiaofuge.deepseek.harness.trigger.http.command;

import cn.xiaofuge.deepseek.harness.api.command.settings.DiscoverModelsRequest;
import cn.xiaofuge.deepseek.harness.api.service.command.HarnessModelDiscoveryApi;
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

    /**
     * 处理Harness 模型 Discovery Controller。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param harnessModelDiscoveryApi harness 模型 Discovery API参数。
     * @return 方法执行结果。
     */
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
