package cn.xiaofuge.deepseek.harness.trigger.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 简单的 API Key 认证拦截器。
 * <p>
 * 当 harness.auth.api-keys 为空时，所有请求直接放行，用于本地开发。
 * 否则要求携带 X-API-Key 或 Bearer Token。
 *
 * <p>调用流程：HTTP/Web 层 → API 适配器 → 应用门面。</p>
 * <p>示例：控制器只负责协议转换，不写业务规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Component
public class ApiKeyAuthInterceptor implements HandlerInterceptor {

    @Value("${harness.auth.api-keys:}")
    private String configuredKeys;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // 放行公开静态资源
        if (path.equals("/") || path.equals("/index.html") || path.equals("/app.js") || path.equals("/app.css")
                || path.startsWith("/lib/") || path.equals("/favicon.ico") || path.startsWith("/actuator")) {
            return true;
        }

        // 未配置密钥时全部放行，用于本地开发模式
        if (configuredKeys == null || configuredKeys.isBlank()) {
            return true;
        }

        // 校验 X-API-Key 或 Authorization 请求头
        String provided = request.getHeader("X-API-Key");
        if (provided == null || provided.isBlank()) {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                provided = auth.substring(7);
            }
        }

        if (provided != null) {
            for (String key : configuredKeys.split(",")) {
                if (provided.trim().equals(key.trim())) return true;
            }
        }

        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Invalid or missing API key\"}");
        return false;
    }
}
