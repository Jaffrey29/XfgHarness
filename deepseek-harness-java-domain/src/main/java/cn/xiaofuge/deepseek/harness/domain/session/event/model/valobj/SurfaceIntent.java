package cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj;

import java.util.List;
import java.util.Objects;

/**
 * Carries the surface-related metadata that may accompany a surface event.
 * <ul>
 *   <li>{@code surfaceOp} — required, determines how the event modifies the surface.</li>
 *   <li>{@code sourceEventSeqs} — optional, the complete set of source event seqs
 *       that produced this surface node. For {@code assistant/message} from a known
 *       empty provider stream this may be an empty list. For other surface event
 *       types, if present, it must be non-empty.</li>
 * </ul>
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SurfaceIntent(
        SurfaceOp surfaceOp,
        List<Long> sourceEventSeqs
) {
    public SurfaceIntent {
        Objects.requireNonNull(surfaceOp, "surfaceOp");
        sourceEventSeqs = sourceEventSeqs == null ? List.of() : List.copyOf(sourceEventSeqs);
    }
}
