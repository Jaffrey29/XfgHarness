package cn.xiaofuge.deepseek.harness.domain.typert.adapter.port;

import java.util.List;
import java.util.Optional;

/**
 * Runtime registry for Host object lookup providers.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ITypertLookupRegistry {

    void register(String key, Object provider);

    Optional<Object> get(String key);

    List<String> keys();
}
