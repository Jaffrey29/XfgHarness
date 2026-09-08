package cn.xiaofuge.deepseek.harness.trigger.service.query;

import cn.xiaofuge.deepseek.harness.api.IHarnessConfigQueryApi;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import cn.xiaofuge.deepseek.harness.cases.config.IHarnessConfigCase;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Web-facing adapter for effective configuration queries.
 */
@Service
public class HarnessConfigQueryApi implements IHarnessConfigQueryApi {

    private final IHarnessConfigCase configCase;

    public HarnessConfigQueryApi(IHarnessConfigCase configCase) {
        this.configCase = configCase;
    }

    @Override
    public Response<Map<String, Object>> effectiveConfig() {
        try {
            return Response.success(configCase.effectiveConfig());
        } catch (RuntimeException exception) {
            return Response.internalError(exception.getMessage());
        }
    }
}
