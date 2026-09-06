package cn.walioffice.deepseek.harness.domain.shared.strategy;

/**
 * Strategy handler: one node in a strategy tree.
 *
 * @param <I> request parameter type
 * @param <C> dynamic context type (mutable, flows through the chain)
 * @param <O> output type
 *
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
