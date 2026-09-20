package cn.xiaofuge.deepseek.harness.trigger.http;

import cn.xiaofuge.deepseek.harness.api.response.Response;
import cn.xiaofuge.deepseek.harness.infrastructure.digitalhuman.CollaborationService;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 协作房间 API：房间、参与者、消息派发、任务取消与统一事件流。
 */
@RestController
@RequestMapping("/api/collaboration")
public class CollaborationController {

    private final CollaborationService collaborationService;

    public CollaborationController(CollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    @PostMapping("/rooms")
    public Response<Map<String, Object>> createRoom(@RequestBody Map<String, Object> body) {
        try {
            return Response.success(collaborationService.createRoom(
                    text(body.get("workspaceId")), text(body.get("title")),
                    text(body.get("objective")), text(body.get("orchestrationMode"))));
        } catch (IllegalArgumentException e) {
            return Response.invalidArgument(e.getMessage());
        }
    }

    @GetMapping("/rooms/{roomId}")
    public Response<Map<String, Object>> roomDetail(@PathVariable("roomId") String roomId) {
        try {
            return Response.success(collaborationService.roomDetail(roomId));
        } catch (IllegalArgumentException e) {
            return Response.invalidArgument(e.getMessage());
        }
    }

    @PostMapping("/rooms/{roomId}/participants")
    public Response<Map<String, Object>> joinRoom(@PathVariable("roomId") String roomId, @RequestBody Map<String, Object> body) {
        try {
            return Response.success(collaborationService.joinRoom(
                    roomId, text(body.get("digitalHumanId")), text(body.get("roomRole"))));
        } catch (IllegalArgumentException e) {
            return Response.invalidArgument(e.getMessage());
        }
    }

    @DeleteMapping("/rooms/{roomId}/participants/{digitalHumanId}")
    public Response<Map<String, Object>> leaveRoom(@PathVariable("roomId") String roomId,
                                                   @PathVariable("digitalHumanId") String digitalHumanId) {
        try {
            return Response.success(collaborationService.leaveRoom(roomId, digitalHumanId));
        } catch (IllegalArgumentException e) {
            return Response.invalidArgument(e.getMessage());
        }
    }

    @PostMapping("/rooms/{roomId}/messages")
    @SuppressWarnings("unchecked")
    public Response<Map<String, Object>> postMessage(@PathVariable("roomId") String roomId, @RequestBody Map<String, Object> body) {
        try {
            List<String> mentions = body.get("mentions") instanceof List<?> list
                    ? list.stream().filter(String.class::isInstance).map(String.class::cast).toList()
                    : List.of();
            Map<String, String> digitalHumanTokens = stringMap(body.get("digitalHumanTokens"));
            // 桌面端随消息下发的沙箱边界：会话归属项目工作目录 + 用户授权的可写根
            List<String> sandboxRoots = body.get("sandboxRoots") instanceof List<?> roots
                    ? roots.stream().filter(String.class::isInstance).map(String.class::cast).toList()
                    : List.of();
            return Response.success(collaborationService.postMessage(
                    roomId, text(body.get("content")), mentions,
                    text(body.get("token")), digitalHumanTokens,
                    text(body.get("channelCode")), text(body.get("approvalMode")),
                    text(body.get("cwd")), sandboxRoots));
        } catch (IllegalArgumentException e) {
            return Response.invalidArgument(e.getMessage());
        }
    }

    @PostMapping("/tasks/{taskId}/cancel")
    public Response<Map<String, Object>> cancelTask(@PathVariable("taskId") String taskId,
                                                    @RequestBody(required = false) Map<String, Object> body) {
        try {
            String roomId = body != null && body.get("roomId") instanceof String value ? value : "";
            return Response.success(collaborationService.cancelTask(roomId, taskId));
        } catch (IllegalArgumentException e) {
            return Response.invalidArgument(e.getMessage());
        }
    }

    @PostMapping("/tasks/{taskId}/resume")
    public Response<Map<String, Object>> resumeTask(@PathVariable("taskId") String taskId,
                                                    @RequestBody(required = false) Map<String, Object> body) {
        try {
            String roomId = body != null && body.get("roomId") instanceof String value ? value : "";
            String token = body != null && body.get("token") instanceof String value ? value : null;
            Map<String, String> digitalHumanTokens = body == null ? Map.of() : stringMap(body.get("digitalHumanTokens"));
            String channelCode = body != null && body.get("channelCode") instanceof String value ? value : null;
            String approvalMode = body != null && body.get("approvalMode") instanceof String value ? value : null;
            return Response.success(collaborationService.resumeTask(roomId, taskId, token, digitalHumanTokens, channelCode, approvalMode));
        } catch (IllegalArgumentException e) {
            return Response.invalidArgument(e.getMessage());
        }
    }

    @PostMapping("/tasks/{taskId}/retry")
    public Response<Map<String, Object>> retryTask(@PathVariable("taskId") String taskId,
                                                   @RequestBody(required = false) Map<String, Object> body) {
        try {
            String roomId = body != null && body.get("roomId") instanceof String value ? value : "";
            String token = body != null && body.get("token") instanceof String value ? value : null;
            Map<String, String> digitalHumanTokens = body == null ? Map.of() : stringMap(body.get("digitalHumanTokens"));
            String channelCode = body != null && body.get("channelCode") instanceof String value ? value : null;
            String approvalMode = body != null && body.get("approvalMode") instanceof String value ? value : null;
            return Response.success(collaborationService.retryTask(roomId, taskId, token, digitalHumanTokens, channelCode, approvalMode));
        } catch (IllegalArgumentException e) {
            return Response.invalidArgument(e.getMessage());
        }
    }

    @PostMapping("/tasks/{taskId}/reassign")
    public Response<Map<String, Object>> reassignTask(@PathVariable("taskId") String taskId,
                                                      @RequestBody(required = false) Map<String, Object> body) {
        try {
            String roomId = body != null && body.get("roomId") instanceof String value ? value : "";
            String targetDigitalHumanId = body != null && body.get("targetDigitalHumanId") instanceof String value ? value : "";
            String token = body != null && body.get("token") instanceof String value ? value : null;
            Map<String, String> digitalHumanTokens = body == null ? Map.of() : stringMap(body.get("digitalHumanTokens"));
            String channelCode = body != null && body.get("channelCode") instanceof String value ? value : null;
            String approvalMode = body != null && body.get("approvalMode") instanceof String value ? value : null;
            return Response.success(collaborationService.reassignTask(roomId, taskId, targetDigitalHumanId, token, digitalHumanTokens, channelCode, approvalMode));
        } catch (IllegalArgumentException e) {
            return Response.invalidArgument(e.getMessage());
        }
    }

    @GetMapping("/rooms/{roomId}/events")
    public Response<List<Map<String, Object>>> listEvents(@PathVariable("roomId") String roomId,
                                                          @RequestParam(value = "afterSeq", defaultValue = "0") long afterSeq) {
        return Response.success(collaborationService.listEvents(roomId, afterSeq));
    }

    /** 房间统一事件流：UI 只连这一条 */
    @GetMapping(value = "/rooms/{roomId}/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@PathVariable("roomId") String roomId,
                                   @RequestParam(value = "afterSeq", defaultValue = "0") long afterSeq) {
        return collaborationService.subscribe(roomId, afterSeq);
    }

    private String text(Object value) {
        return value instanceof String string && !string.isBlank() ? string.trim() : null;
    }

    private Map<String, String> stringMap(Object value) {
        if (!(value instanceof Map<?, ?> raw)) return Map.of();
        java.util.LinkedHashMap<String, String> result = new java.util.LinkedHashMap<>();
        raw.forEach((key, entry) -> {
            if (key instanceof String textKey && entry instanceof String textValue && !textValue.isBlank()) {
                result.put(textKey, textValue);
            }
        });
        return result;
    }
}
