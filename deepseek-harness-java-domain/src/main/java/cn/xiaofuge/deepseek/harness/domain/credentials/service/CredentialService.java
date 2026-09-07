package cn.xiaofuge.deepseek.harness.domain.credentials.service;

import cn.xiaofuge.deepseek.harness.domain.credentials.ICredentialService;
import cn.xiaofuge.deepseek.harness.domain.credentials.adapter.port.ICredentialProviderPort;
import cn.xiaofuge.deepseek.harness.domain.credentials.model.valobj.CredentialInfoVO;
import cn.xiaofuge.deepseek.harness.domain.credentials.model.valobj.CredentialRef;
import cn.xiaofuge.deepseek.harness.domain.credentials.model.valobj.ResolvedCredentialVO;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Delegates to a configured {@link ICredentialProviderPort}; the port owns all
 * source-layer resolution and storage semantics.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class CredentialService implements ICredentialService {

    private final ICredentialProviderPort providerPort;

    /**
     * 处理Credential Service相关逻辑。
     */
    public CredentialService(ICredentialProviderPort providerPort) {
        this.providerPort = providerPort;
    }

    @Override
    public Optional<ResolvedCredentialVO> resolve(CredentialRef ref) {
        return providerPort.resolve(ref);
    }

    @Override
    public CredentialInfoVO describe(CredentialRef ref) {
    /**
     * 设置。
     */
        return providerPort.describe(ref);
    }

    @Override
    public void set(CredentialRef ref, String value) {
        providerPort.set(ref, value);
    }

    @Override
    public void unset(CredentialRef ref) {
        providerPort.unset(ref);
    }
}
