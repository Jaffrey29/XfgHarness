package cn.xiaofuge.deepseek.harness.domain.typert.model.valobj;

/**
 * Result of a Remote method call: either ok with a value, or error with a failure.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public sealed interface RemoteResult permits RemoteResult.Ok, RemoteResult.Error {

    /**
     * Ok 定义当前模块中的record契约与职责。
     */
    record Ok(Object value) implements RemoteResult {}

    /**
     * Error 定义当前模块中的record契约与职责。
     */
    record Error(RemoteFailure failure) implements RemoteResult {}

    static RemoteResult ok(Object value) { return new Ok(value); }
    static RemoteResult error(RemoteFailure failure) { return new Error(failure); }
}
