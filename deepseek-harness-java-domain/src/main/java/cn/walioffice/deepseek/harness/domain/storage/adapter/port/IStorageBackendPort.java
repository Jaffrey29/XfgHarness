package cn.walioffice.deepseek.harness.domain.storage.adapter.port;

import cn.walioffice.deepseek.harness.domain.storage.model.valobj.KvSnapshot;
import cn.walioffice.deepseek.harness.domain.storage.model.valobj.KvUnitDescriptor;
import java.util.Map;

/**
 * Port for a storage backend that owns the physical medium for KV units.
 *
 * <p>One unit maps to one logical file (json) or one logical database (sqlite).
 * The backend handles load, persist, and delete for individual keys.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IStorageBackendPort {

    /**
     * Load all tables and keys for the given unit descriptor.
     *
     * @param descriptor unit descriptor
     * @return snapshot of all tables
     */
    KvSnapshot load(KvUnitDescriptor descriptor);

    /**
     * Persist a single key within a table.
     *
     * @param descriptor unit descriptor
     * @param table      table name
     * @param key        key within the table
     * @param value      value to store
     */
    void put(KvUnitDescriptor descriptor, String table, String key, String value);

    /**
     * Remove a single key within a table.
     *
     * @param descriptor unit descriptor
     * @param table      table name
     * @param key        key within the table
     */
    void remove(KvUnitDescriptor descriptor, String table, String key);

    /**
     * Persist a batch of puts atomically.
     *
     * @param descriptor unit descriptor
     * @param puts       map of table → (key → value)
     */
    void putAll(KvUnitDescriptor descriptor, Map<String, Map<String, String>> puts);
}
