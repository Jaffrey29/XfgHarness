package cn.walioffice.deepseek.harness.domain.storage.service;

import cn.walioffice.deepseek.harness.domain.storage.IStorageService;
import cn.walioffice.deepseek.harness.domain.storage.adapter.port.IStorageBackendPort;
import cn.walioffice.deepseek.harness.domain.storage.model.valobj.KvSnapshot;
import cn.walioffice.deepseek.harness.domain.storage.model.valobj.KvUnitDescriptor;
import java.util.Map;
import java.util.Optional;

/**
 * Delegates to a configured {@link IStorageBackendPort}; the port owns all
 * physical-medium semantics (file layout, atomicity, permissions).
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class StorageService implements IStorageService {

    private final IStorageBackendPort backendPort;

    public StorageService(IStorageBackendPort backendPort) {
        this.backendPort = backendPort;
    }

    @Override
    public KvSnapshot load(KvUnitDescriptor descriptor) {
        return backendPort.load(descriptor);
    }

    @Override
    public Optional<String> get(KvUnitDescriptor descriptor, String table, String key) {
        KvSnapshot snapshot = backendPort.load(descriptor);
        Map<String, String> tableData = snapshot.tables().get(table);
        if (tableData == null) return Optional.empty();
        return Optional.ofNullable(tableData.get(key));
    }

    @Override
    public void put(KvUnitDescriptor descriptor, String table, String key, String value) {
        backendPort.put(descriptor, table, key, value);
    }

    @Override
    public void remove(KvUnitDescriptor descriptor, String table, String key) {
        backendPort.remove(descriptor, table, key);
    }

    @Override
    public void putAll(KvUnitDescriptor descriptor, Map<String, Map<String, String>> puts) {
        backendPort.putAll(descriptor, puts);
    }
}
