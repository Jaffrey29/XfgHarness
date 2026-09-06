package cn.xiaofuge.deepseek.harness.domain.sdk.adapter.port;

/**
 * Port for the server-side request handler.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISdkServerHandler {

    /**
     * Handle an incoming JSON-RPC request.
     *
     * @param method  request method
     * @param params  request params
     * @return result object (JSON-serializable)
     */
    Object handle(String method, Object params);
}
