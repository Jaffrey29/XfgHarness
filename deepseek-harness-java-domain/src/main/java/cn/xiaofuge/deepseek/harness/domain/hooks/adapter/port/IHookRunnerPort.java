package cn.xiaofuge.deepseek.harness.domain.hooks.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj.CommandHook;
import cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj.HookOutput;
import java.util.Map;

/**
 * Port for running a single hook command and parsing its output.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IHookRunnerPort {

    /**
     * Run one command hook with the given environment and stdin payload.
     *
     * @param hook       the command hook
     * @param payloadJson  stdin JSON payload (hook event data)
     * @param environment   environment variables for the hook process
     * @return parsed hook output
     */
    HookOutput run(CommandHook hook, String payloadJson, Map<String, String> environment);
}
