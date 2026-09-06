package cn.xiaofuge.deepseek.harness.domain.typert.adapter.port;

import java.util.Optional;

/**
 * Runtime registry for Host Context resolvers and Client Context binders.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ITypertContextRegistry {

    void registerHost(String key, Object resolver);

    Optional<Object> getHost(String key);

    void registerClient(String key, Object binder);

    Optional<Object> getClient(String key);
}
