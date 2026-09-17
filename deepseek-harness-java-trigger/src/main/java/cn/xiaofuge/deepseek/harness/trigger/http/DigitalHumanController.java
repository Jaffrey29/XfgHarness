package cn.xiaofuge.deepseek.harness.trigger.http;

import cn.xiaofuge.deepseek.harness.api.response.Response;
import cn.xiaofuge.deepseek.harness.infrastructure.digitalhuman.DigitalHumanService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数字人目录 API：身份 + 端点管理、Agent Card 探测、健康检查。
 *
 * <p>凭据明文由调用方随请求带来（探测/健康检查一次性使用），
 * 本服务只保存 credentialRef 引用，不落明文。</p>
 */
@RestController
@RequestMapping("/api/digital-humans")
public class DigitalHumanController {

    private final DigitalHumanService digitalHumanService;

    public DigitalHumanController(DigitalHumanService digitalHumanService) {
        this.digitalHumanService = digitalHumanService;
    }

    @GetMapping
    public Response<List<Map<String, Object>>> list() {
        return Response.success(digitalHumanService.list());
    }

    @GetMapping("/{id}")
    public Response<Map<String, Object>> get(@PathVariable("id") String id) {
        try {
            return Response.success(digitalHumanService.get(id));
        } catch (IllegalArgumentException e) {
            return Response.invalidArgument(e.getMessage());
        }
    }

    @PostMapping
    public Response<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        try {
            return Response.success(digitalHumanService.create(body));
        } catch (IllegalArgumentException e) {
            return Response.invalidArgument(e.getMessage());
        } catch (Exception e) {
            return Response.internalError("创建数字人失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Response<Map<String, Object>> update(@PathVariable("id") String id, @RequestBody Map<String, Object> body) {
        try {
            return Response.success(digitalHumanService.update(id, body));
        } catch (IllegalArgumentException e) {
            return Response.invalidArgument(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable("id") String id) {
        digitalHumanService.delete(id);
        return Response.success(null);
    }

    @PostMapping("/discover")
    public Response<Map<String, Object>> discover(@RequestBody Map<String, Object> body) {
        String baseUrl = body.get("baseUrl") instanceof String value ? value : null;
        String credentialRef = body.get("credentialRef") instanceof String value ? value : null;
        // 凭据明文一次性使用，不保存
        String token = body.get("token") instanceof String value ? value : null;
        return Response.success(digitalHumanService.discover(baseUrl, credentialRef, token));
    }

    @PostMapping("/{id}/health-check")
    public Response<Map<String, Object>> healthCheck(@PathVariable("id") String id, @RequestBody(required = false) Map<String, Object> body) {
        try {
            String token = body != null && body.get("token") instanceof String value ? value : null;
            return Response.success(digitalHumanService.healthCheck(id, token));
        } catch (IllegalArgumentException e) {
            return Response.invalidArgument(e.getMessage());
        }
    }
}
