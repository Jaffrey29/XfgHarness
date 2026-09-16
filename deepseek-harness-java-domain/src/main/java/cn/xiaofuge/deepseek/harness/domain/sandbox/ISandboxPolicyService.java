package cn.xiaofuge.deepseek.harness.domain.sandbox;

import cn.xiaofuge.deepseek.harness.domain.sandbox.model.valobj.SandboxExecutionPolicy;
import cn.xiaofuge.deepseek.harness.domain.sandbox.model.valobj.SandboxPolicyRequest;

import java.nio.file.Path;
import java.util.List;

/**
 * Application-facing sandbox-policy seam. The single owner of the deployment's
 * sandbox fallbacks plus per-session resolution.
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISandboxPolicyService {

    /**
     * 处理resolve。
     * <p>流程：校验入参 → 执行领域动作 → 返回处理结果。</p>
     * @param request 请求。
     * @return 领域处理结果。
     */
    SandboxExecutionPolicy resolve(SandboxPolicyRequest request);

    /**
     * 在会话工作区之外附加用户授权的可写根（如项目下挂载的工程目录），
     * 仅对 WORKSPACE_WRITE 模式有意义。
     * @param request 请求。
     * @param extraRoots 额外可写根（绝对路径）。
     * @return 领域处理结果。
     */
    default SandboxExecutionPolicy resolveWithExtraRoots(SandboxPolicyRequest request, List<Path> extraRoots) {
        return resolve(request);
    }
}
