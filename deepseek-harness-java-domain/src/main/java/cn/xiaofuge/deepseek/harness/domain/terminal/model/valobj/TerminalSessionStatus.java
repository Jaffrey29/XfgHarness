package cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj;

/**
 * 终端会话状态。
 * <p>
 * 执行流程：
 * 1. 后端进程启动后进入 running；
 * 2. 进程退出后进入 exited；
 * 3. 显式关闭后进入 closed。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public enum TerminalSessionStatus {
    RUNNING,
    EXITED,
    CLOSED
}
