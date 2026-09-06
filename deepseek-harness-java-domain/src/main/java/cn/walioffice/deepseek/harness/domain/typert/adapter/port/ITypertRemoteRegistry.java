package cn.walioffice.deepseek.harness.domain.typert.adapter.port;

import cn.walioffice.deepseek.harness.domain.typert.model.entity.InvocationDescriptor;
import java.util.List;
import java.util.Optional;

/**
 * Remote invocation registry: tracks Remote method descriptors.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ITypertRemoteRegistry {

    void register(InvocationDescriptor descriptor);

    Optional<InvocationDescriptor> get(String endpoint);

    List<InvocationDescriptor> list();
}
