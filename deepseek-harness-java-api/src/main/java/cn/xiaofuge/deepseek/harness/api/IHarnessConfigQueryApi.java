package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.response.Response;
import java.util.Map;

/**
 * API for reading the effective harness configuration.
 */
public interface IHarnessConfigQueryApi {

    Response<Map<String, Object>> effectiveConfig();
}
