package cn.walioffice.deepseek.harness.domain.session.event.model.valobj;

import java.util.Objects;

/**
 * Describes how a surface event modifies the visible conversation surface.
 * <ul>
 *   <li>{@code APPEND} — add a new node to the end of the surface.</li>
 *   <li>{@code REPLACE} — replace surface nodes {@code [start..end]} (inclusive)
 *       with this event. Both endpoints must be current surface node seqs.
 *       {@code start == end} replaces a single node.</li>
 * </ul>
 * <p>
 * The {@code sourceEventSeqs} of a replace event MUST cover all seqs of the
 * surface nodes being obscured.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public sealed interface SurfaceOp permits SurfaceOp.Append, SurfaceOp.Replace {

    /**
     * Append a new node to the surface.
     */
    record Append() implements SurfaceOp {}

    /**
     * Replace existing surface nodes [start..end] (inclusive).
     *
     * @param start seq of the first surface node to replace
     * @param end   seq of the last surface node to replace
     */
    record Replace(long start, long end) implements SurfaceOp {
        public Replace {
            if (start < 0 || end < 0 || end < start) {
                throw new IllegalArgumentException("Invalid replace range: start=" + start + ", end=" + end);
            }
        }
    }

    /**
     * Factory for append.
     */
    static SurfaceOp append() {
        return new Append();
    }

    /**
     * Factory for replace.
     */
    static SurfaceOp replace(long start, long end) {
        return new Replace(start, end);
    }
}
