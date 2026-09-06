package cn.walioffice.deepseek.harness.domain.typert.service;

import cn.walioffice.deepseek.harness.domain.typert.ITypertService;
import cn.walioffice.deepseek.harness.domain.typert.adapter.port.*;
import cn.walioffice.deepseek.harness.domain.typert.model.entity.InvocationDescriptor;
import cn.walioffice.deepseek.harness.domain.typert.model.entity.TypertContribution;
import java.util.List;
import java.util.Optional;

/**
 * Typert service: owns the registry contract and manages contributions.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class TypertService implements ITypertService {

    private final ITypertRegistryContract registryContract;

    public TypertService(ITypertRegistryContract registryContract) {
        this.registryContract = registryContract;
    }

    @Override
    public void registerContribution(TypertContribution contribution) {
        for (InvocationDescriptor desc : contribution.invocations()) {
            registryContract.remotes().register(desc);
            registryContract.local().register(desc);
        }
    }

    @Override
    public Optional<InvocationDescriptor> resolveInvocation(String endpoint) {
        return registryContract.remotes().get(endpoint);
    }

    @Override
    public List<InvocationDescriptor> listInvocations() {
        return registryContract.remotes().list();
    }

    @Override
    public ITypertRegistryContract registry() {
        return registryContract;
    }
}
