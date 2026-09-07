package cn.xiaofuge.deepseek.harness.domain.hooks.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj.CommandHook;
import cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj.HookOutput;
import java.util.Map;

/**
 * Port for running a single hook command and parsing its output.
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
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
