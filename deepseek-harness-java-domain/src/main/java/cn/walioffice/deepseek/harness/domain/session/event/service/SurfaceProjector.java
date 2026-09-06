package cn.walioffice.deepseek.harness.domain.session.event.service;

import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.walioffice.deepseek.harness.domain.session.event.model.valobj.SurfaceEventType;
import cn.walioffice.deepseek.harness.domain.session.event.model.valobj.SurfaceIntent;
import cn.walioffice.deepseek.harness.domain.session.event.model.valobj.SurfaceOp;
import java.util.ArrayList;
import java.util.List;

/**
 * Projects the session event log into an ordered list of "surface nodes" —
 * the visible conversation surface (user messages, assistant messages, tool results).
 * <p>
 * This is the read-side projection that powers conversation views.
 * Surface events with {@code SurfaceOp.APPEND} add nodes; events with
 * {@code SurfaceOp.REPLACE} replace existing nodes.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class SurfaceProjector {

    /**
     * A surface node is a reference to the source event that produced it.
     */
    public record SurfaceNode(long seq, SessionEvent event) {}

    /**
     * Project all surface events from the given event list into surface nodes.
     *
     * @param events ordered session events
     * @return ordered surface nodes
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
     * Extract only user and assistant messages from surface nodes
     * (excluding tool results for chat display).
     */
    public List<SessionEvent> extractChatMessages(List<SessionEvent> events) {
        List<SurfaceNode> nodes = project(events);
        return nodes.stream()
                .map(SurfaceNode::event)
                .filter(e -> e.type() == cn.walioffice.deepseek.harness.domain.session.event.model.valobj.SessionEventType.USER_MESSAGE
                        || e.type() == cn.walioffice.deepseek.harness.domain.session.event.model.valobj.SessionEventType.ASSISTANT_MESSAGE)
                .toList();
    }
}
