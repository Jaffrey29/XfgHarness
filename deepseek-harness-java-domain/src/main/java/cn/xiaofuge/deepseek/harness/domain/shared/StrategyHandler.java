package cn.xiaofuge.deepseek.harness.domain.shared;

/**
 * Strategy handler: one node in a strategy tree.
 *
 * @param <I> request parameter type
 * @param <C> dynamic context type (mutable, flows through the chain)
 * @param <O> output type
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@FunctionalInterface
public interface StrategyHandler<I, C, O> {

    /**
     * Apply this node's logic and return the result.
     *
     * @param requestParameter the request parameter
     * @param dynamicContext   the dynamic context flowing through the chain
     * @return the output
     * @throws Exception if the node fails
     */
    O apply(I requestParameter, C dynamicContext) throws Exception;
}
