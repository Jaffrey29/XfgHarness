package cn.xiaofuge.deepseek.harness.domain.storage.model.valobj;

import java.util.Map;

/**
 * Immutable snapshot of all tables in a KV unit at load time.
 *
 * @param tables table name → (key → value)
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record KvSnapshot(Map<String, Map<String, String>> tables) {
    public KvSnapshot {
        tables = tables == null ? Map.of() : Map.copyOf(tables);
    }
}
