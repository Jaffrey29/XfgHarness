package cn.walioffice.deepseek.harness.domain.storage;

import cn.walioffice.deepseek.harness.domain.storage.model.valobj.KvSnapshot;
import cn.walioffice.deepseek.harness.domain.storage.model.valobj.KvUnitDescriptor;
import java.util.Map;
import java.util.Optional;

/**
 * Application-facing KV storage seam. Each unit is a named logical store with
 * typed tables; the backend owns the physical medium.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IStorageService {

    KvSnapshot load(KvUnitDescriptor descriptor);

    Optional<String> get(KvUnitDescriptor descriptor, String table, String key);

    void put(KvUnitDescriptor descriptor, String table, String key, String value);

    void remove(KvUnitDescriptor descriptor, String table, String key);

    void putAll(KvUnitDescriptor descriptor, Map<String, Map<String, String>> puts);
}
