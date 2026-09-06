package cn.xiaofuge.deepseek.harness.domain.e2b.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.e2b.model.valobj.E2BExecResult;

/**
 * Port for running subprocesses inside an E2B sandbox.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IE2BSubprocessPort {

    /**
     * Run a command in the sandbox.
     *
     * @param sandboxId sandbox identifier
     * @param command   command to run
     * @return execution result
     */
    E2BExecResult run(String sandboxId, String command);

    /**
     * Run a command with stdin input.
     *
     * @param sandboxId sandbox identifier
     * @param command   command to run
     * @param stdin     stdin content
     * @return execution result
     */
    E2BExecResult runWithInput(String sandboxId, String command, String stdin);
}
