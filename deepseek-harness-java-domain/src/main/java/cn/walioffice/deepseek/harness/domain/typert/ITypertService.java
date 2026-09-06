package cn.walioffice.deepseek.harness.domain.typert;

import cn.walioffice.deepseek.harness.domain.typert.adapter.port.ITypertRegistryContract;
import cn.walioffice.deepseek.harness.domain.typert.model.entity.InvocationDescriptor;
import cn.walioffice.deepseek.harness.domain.typert.model.entity.TypertContribution;
import java.util.List;
import java.util.Optional;

/**
 * Application-facing Typert seam: manages generated reflection, Remote
 * invocations, and lookup/context providers.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ITypertService {

    void registerContribution(TypertContribution contribution);

    Optional<InvocationDescriptor> resolveInvocation(String endpoint);

    List<InvocationDescriptor> listInvocations();

    ITypertRegistryContract registry();
}
