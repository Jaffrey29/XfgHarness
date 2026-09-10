package cn.xiaofuge.deepseek.harness.trigger.http.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 插件命令接口访问控制。
 * 本机调用默认放行；远程调用必须携带 X-Plugin-Api-Token。
 */
@Component
@Order(10)
public class PluginApiAuthFilter implements Filter {

    public static final String TOKEN_HEADER = "X-Plugin-Api-Token";
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final String configuredToken;

    public PluginApiAuthFilter(@Value("${harness.plugins.api-token:}") String configuredToken) {
        this.configuredToken = configuredToken == null ? "" : configuredToken.trim();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest servletRequest)
                || !(response instanceof HttpServletResponse servletResponse)
                || !servletRequest.getRequestURI().startsWith("/api/harness/plugins/")
                && !servletRequest.getRequestURI().equals("/api/harness/plugins")
                || READ_METHODS.contains(servletRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        if (isLoopback(servletRequest) || isAuthorized(servletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        servletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        servletResponse.setContentType("application/json;charset=UTF-8");
        servletResponse.getWriter().write(
                "{\"code\":\"A0300\",\"info\":\"plugin API token required\",\"data\":null}");
    }

    private boolean isLoopback(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return "127.0.0.1".equals(remoteAddr) || "0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr);
    }

    private boolean isAuthorized(HttpServletRequest request) {
        return !configuredToken.isBlank()
                && configuredToken.equals(request.getHeader(TOKEN_HEADER));
    }
}
