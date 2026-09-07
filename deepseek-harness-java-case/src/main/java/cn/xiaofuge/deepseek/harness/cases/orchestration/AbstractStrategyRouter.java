package cn.xiaofuge.deepseek.harness.cases.orchestration;

/**
 * Abstract strategy router for case orchestration.
 *
 * @param <I> request parameter type
 * @param <C> dynamic context type
 * @param <O> output type
 */
public abstract class AbstractStrategyRouter<I, C, O> implements StrategyHandler<I, C, O> {

    protected StrategyHandler<I, C, O> defaultStrategyHandler = (request, context) -> null;

    protected abstract O doApply(I requestParameter, C dynamicContext) throws Exception;

    public abstract StrategyHandler<I, C, O> getNext(I requestParameter, C dynamicContext) throws Exception;

    @Override
    public final O apply(I requestParameter, C dynamicContext) throws Exception {
        O result = doApply(requestParameter, dynamicContext);
        if (result != null) {
            return result;
        }
        return router(requestParameter, dynamicContext);
    }

    protected O router(I requestParameter, C dynamicContext) throws Exception {
        return getNext(requestParameter, dynamicContext).apply(requestParameter, dynamicContext);
    }
}
