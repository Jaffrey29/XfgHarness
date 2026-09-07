package cn.xiaofuge.deepseek.harness.domain.session.event.service;

import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj.SessionEventType;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj.SurfaceIntent;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj.SurfaceOp;
import java.util.ArrayList;
import java.util.List;

/**
 * SurfaceProjector 表示SurfaceProjector。
 */
public class SurfaceProjector {

    /**
     * SurfaceNode 表示SurfaceNode。
     */
    public record SurfaceNode(long seq, SessionEvent event) {}

    /**
     * 处理project。
     * <p>流程：校验入参 → 执行领域动作 → 返回处理结果。</p>
     * @param events 事件列表。
     * @return 领域处理结果。
     */
    public List<SurfaceNode> project(List<SessionEvent> events) {
        List<SurfaceNode> surface = new ArrayList<>();
        for (SessionEvent event : events) {
            if (!event.isSurfaceEvent()) {
                continue;
            }
            SurfaceIntent intent = event.surfaceIntent();
            if (intent == null) {
                // No surface intent — treat as append (backward compat)
                surface.add(new SurfaceNode(event.seq(), event));
                continue;
            }
            if (intent.surfaceOp() instanceof SurfaceOp.Append) {
                surface.add(new SurfaceNode(event.seq(), event));
            } else if (intent.surfaceOp() instanceof SurfaceOp.Replace r) {
                // Remove nodes in [r.start(), r.end()]
                surface.removeIf(node -> node.seq() >= r.start() && node.seq() <= r.end());
                surface.add(new SurfaceNode(event.seq(), event));
            }
        }
        return surface;
    }

    /**
     * 处理extractChatMessages消息列表。
     * <p>流程：校验入参 → 执行领域动作 → 返回处理结果。</p>
     * @param events 事件列表。
     * @return 领域处理结果。
     */
    public List<SessionEvent> extractChatMessages(List<SessionEvent> events) {
        List<SurfaceNode> nodes = project(events);
        return nodes.stream()
                .map(SurfaceNode::event)
                .filter(e -> e.type() == SessionEventType.USER_MESSAGE
                        || e.type() == SessionEventType.ASSISTANT_MESSAGE)
                .toList();
    }
}
