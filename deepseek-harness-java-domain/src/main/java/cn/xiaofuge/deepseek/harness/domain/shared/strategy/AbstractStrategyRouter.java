package cn.xiaofuge.deepseek.harness.domain.shared.strategy;

/**
 * Abstract strategy router: a node that does work in {@link #doApply} and then
 * routes to the next node returned by {@link #getNext}.
 *
 * <p>Mirrors the pattern from ai-mcp-gateway's
 * {@code AbstractMultiThreadStrategyRouter}: each node performs its business
 * logic and delegates to the next node via {@link #router}. The default
 * terminal handler returns the result as-is.
 *
 * @param <I> request parameter type
 * @param <C> dynamic context type
 * @param <O> output type
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public abstract class AbstractStrategyRouter<I, C, O> implements StrategyHandler<I, C, O> {

    /**
     * Terminal handler: returns null (leaf nodes route here).
     * Override or reassign for custom terminal behavior.
     */
    protected StrategyHandler<I, C, O> defaultStrategyHandler = (req, ctx) -> null;

    /**
     * Node business logic. Subclasses implement this — NOT {@link #apply}.
     *
     * @param requestParameter the request parameter
     * @param dynamicContext   the dynamic context
     * @return the output (may be ignored if routing continues)
     * @throws Exception if the node fails
     */
    protected abstract O doApply(I requestParameter, C dynamicContext) throws Exception;

    /**
     * Return the next node in the chain.
     *
     * @param requestParameter the request parameter
     * @param dynamicContext   the dynamic context
     * @return the next strategy handler, or {@link #defaultStrategyHandler} at leaf
     * @throws Exception if resolution fails
     */
    public abstract StrategyHandler<I, C, O> getNext(I requestParameter, C dynamicContext) throws Exception;

    @Override
    public final O apply(I requestParameter, C dynamicContext) throws Exception {
        O result = doApply(requestParameter, dynamicContext);
        // If the node returned a non-null result, short-circuit (e.g. 404, error)
        if (result != null) return result;
        return router(requestParameter, dynamicContext);
    }

    /**
     * Route to the next node. Called after {@link #doApply} returns null.
     */
    protected O router(I requestParameter, C dynamicContext) throws Exception {
        return getNext(requestParameter, dynamicContext).apply(requestParameter, dynamicContext);
    }
}
