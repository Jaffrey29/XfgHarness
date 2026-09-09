package cn.xiaofuge.deepseek.harness.trigger.http.query;

import cn.xiaofuge.deepseek.harness.api.response.Response;
import cn.xiaofuge.deepseek.harness.trigger.service.query.HarnessPluginConfigQueryApi;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/harness/plugins/{pluginId}/config")
public class HarnessPluginConfigQueryController {

    private final HarnessPluginConfigQueryApi pluginConfigQueryApi;

    public HarnessPluginConfigQueryController(HarnessPluginConfigQueryApi pluginConfigQueryApi) {
        this.pluginConfigQueryApi = pluginConfigQueryApi;
    }

    @GetMapping
    public Response<List<Map<String, String>>> get(@PathVariable("pluginId") String pluginId) {
        return pluginConfigQueryApi.list(pluginId);
    }
}
