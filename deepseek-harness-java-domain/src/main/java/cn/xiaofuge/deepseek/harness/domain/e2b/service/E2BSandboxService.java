package cn.xiaofuge.deepseek.harness.domain.e2b.service;

import cn.xiaofuge.deepseek.harness.domain.e2b.IE2BSandboxService;
import cn.xiaofuge.deepseek.harness.domain.e2b.adapter.port.IE2BFsPort;
import cn.xiaofuge.deepseek.harness.domain.e2b.adapter.port.IE2BSubprocessPort;
import cn.xiaofuge.deepseek.harness.domain.e2b.model.valobj.E2BSandboxEnv;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * E2B sandbox service: manages sandbox lifecycle and routes to per-sandbox ports.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class E2BSandboxService implements IE2BSandboxService {

    private final ConcurrentHashMap<String, E2BSandboxEnv> sandboxes = new ConcurrentHashMap<>();
    private final IE2BFsPort fsPort;
    private final IE2BSubprocessPort subprocessPort;

    public E2BSandboxService(IE2BFsPort fsPort, IE2BSubprocessPort subprocessPort) {
        this.fsPort = fsPort;
        this.subprocessPort = subprocessPort;
    }

    @Override
    public String create(E2BSandboxEnv env) {
        String id = "e2b_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        sandboxes.put(id, env);
        return id;
    }

    @Override
    public Optional<IE2BFsPort> fs(String sandboxId) {
        return sandboxes.containsKey(sandboxId) ? Optional.of(fsPort) : Optional.empty();
    }

    @Override
    public Optional<IE2BSubprocessPort> subprocess(String sandboxId) {
        return sandboxes.containsKey(sandboxId) ? Optional.of(subprocessPort) : Optional.empty();
    }

    @Override
    public void kill(String sandboxId) {
        sandboxes.remove(sandboxId);
    }
}
