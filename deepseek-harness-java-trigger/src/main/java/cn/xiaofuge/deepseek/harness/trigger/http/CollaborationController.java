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
            return Response.success(collaborationService.postMessage(
                    roomId, text(body.get("content")), mentions,
                    text(body.get("token")), text(body.get("channelCode")), text(body.get("approvalMode"))));
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
            String channelCode = body != null && body.get("channelCode") instanceof String value ? value : null;
            String approvalMode = body != null && body.get("approvalMode") instanceof String value ? value : null;
            return Response.success(collaborationService.resumeTask(roomId, taskId, token, channelCode, approvalMode));
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
}
