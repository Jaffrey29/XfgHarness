package cn.walioffice.deepseek.harness.domain.tool.model.valobj;

/**
 * 待执行工具调用的调度模式。
 * 对应 TS 侧 {@code ToolExecutionMode}。
 *
 * <ul>
 *   <li>{@link #PARALLEL} — 可以在有界线程池中与同批调用并行执行</li>
 *   <li>{@link #EXCLUSIVE} — 单独执行，并形成顺序屏障</li>
 * </ul>
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public enum ToolExecutionMode {
    PARALLEL,
    EXCLUSIVE
}
