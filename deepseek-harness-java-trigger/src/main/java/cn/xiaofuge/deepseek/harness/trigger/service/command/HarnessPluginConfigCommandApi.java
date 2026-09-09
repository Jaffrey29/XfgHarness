package cn.xiaofuge.deepseek.harness.trigger.service.command;

import cn.xiaofuge.deepseek.harness.api.response.Response;
import cn.xiaofuge.deepseek.harness.cases.plugin.config.IPluginConfigCase;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class HarnessPluginConfigCommandApi {

    private final IPluginConfigCase pluginConfigCase;

    public HarnessPluginConfigCommandApi(IPluginConfigCase pluginConfigCase) {
        this.pluginConfigCase = pluginConfigCase;
    }

    public Response<Void> save(String pluginId, List<Map<String, String>> configs) {
        try {
            if (pluginId == null || pluginId.isBlank()) {
                return Response.invalidArgument("pluginId must not be blank");
            }
            pluginConfigCase.save(pluginId, configs);
            return Response.success(null);
        } catch (RuntimeException exception) {
            return Response.internalError(exception.getMessage());
        }
    }
}
