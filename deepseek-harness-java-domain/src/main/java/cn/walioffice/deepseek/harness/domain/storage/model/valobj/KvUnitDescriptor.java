package cn.walioffice.deepseek.harness.domain.storage.model.valobj;

import java.util.Set;

/**
 * Describes a KV unit: a named logical store with typed tables.
 *
 * @param name   unit name (POSIX identifier)
 * @param tables table names within the unit
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record KvUnitDescriptor(String name, Set<String> tables) {
    public KvUnitDescriptor {
        tables = tables == null ? Set.of() : Set.copyOf(tables);
    }
}
