package cn.walioffice.deepseek.harness.infrastructure.adapter.credentials;

import cn.walioffice.deepseek.harness.domain.credentials.adapter.port.ICredentialProviderPort;
import cn.walioffice.deepseek.harness.domain.credentials.model.valobj.CredentialInfoVO;
import cn.walioffice.deepseek.harness.domain.credentials.model.valobj.CredentialRef;
import cn.walioffice.deepseek.harness.domain.credentials.model.valobj.ResolvedCredentialVO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;


/**
 * File-backed credentials provider over {@code ~/.dsh/.credentials.yaml}, layered
 * against the environment by precedence:
 * <pre>
 * inherited process environment      (read-only, wins)
 * > ~/.dsh/.credentials.yaml         (provider-managed, writable)
 * > &lt;cwd&gt;/.env                       (read-only fallback)
 * </pre>
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class LocalCredentialProviderPort implements ICredentialProviderPort {

    private static final String CREDENTIALS_FILE = ".credentials.yaml";
    private static final String ENV_FILE = ".env";
    private final Path credentialsPath;
    private final Path envPath;
    private final ConcurrentHashMap<String, String> managedStore = new ConcurrentHashMap<>();
    private final Map<String, String> envCache = new ConcurrentHashMap<>();

    public LocalCredentialProviderPort(Path dshHome) {
        this.credentialsPath = dshHome.resolve(CREDENTIALS_FILE);
        this.envPath = dshHome.resolve(ENV_FILE);
        if (!Files.exists(dshHome)) {
            try {
                Files.createDirectories(dshHome);
            } catch (Exception e) {
                // non-fatal at bootstrap
            }
        }
    }

    
    public void init() {
        loadManagedStore();
        loadEnvFallback();
    }

    @Override
    public Optional<ResolvedCredentialVO> resolve(CredentialRef ref) {
        String key = ref.value();
        // 1. inherited process environment (read-only, highest priority)
        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return Optional.of(new ResolvedCredentialVO(fromEnv, "env"));
        }
        // 2. managed store (.credentials.yaml)
        String fromManaged = managedStore.get(key);
        if (fromManaged != null && !fromManaged.isBlank()) {
            return Optional.of(new ResolvedCredentialVO(fromManaged, "file"));
        }
        // 3. env file fallback
        String fromEnvFile = envCache.get(key);
        if (fromEnvFile != null && !fromEnvFile.isBlank()) {
            return Optional.of(new ResolvedCredentialVO(fromEnvFile, "project-env"));
        }
        return Optional.empty();
    }

    @Override
    public CredentialInfoVO describe(CredentialRef ref) {
        Optional<ResolvedCredentialVO> resolved = resolve(ref);
        boolean configured = resolved.isPresent();
        String source = configured ? resolved.get().source() : null;
        boolean writable = !isShadowedByEnv(ref) && managedStore.containsKey(ref.value()) || !Files.exists(credentialsPath);
        return new CredentialInfoVO(configured, Optional.ofNullable(source), writable);
    }

    @Override
    public void set(CredentialRef ref, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("credential value must not be blank");
        }
        assertUnshadowed(ref);
        managedStore.put(ref.value(), value);
        writeCredentialsFile();
    }

    @Override
    public void unset(CredentialRef ref) {
        assertUnshadowed(ref);
        managedStore.remove(ref.value());
        writeCredentialsFile();
    }

    private void assertUnshadowed(CredentialRef ref) {
        if (System.getenv(ref.value()) != null) {
            throw new IllegalStateException(
                    "credential \"" + ref.value() + "\" is supplied by the process environment and cannot be written");
        }
    }

    private boolean isShadowedByEnv(CredentialRef ref) {
        return System.getenv(ref.value()) != null;
    }

    private void loadManagedStore() {
        if (!Files.exists(credentialsPath)) return;
        try {
            Map<String, String> parsed = CredentialsYamlStore.parse(credentialsPath);
            parsed.forEach(managedStore::put);
        } catch (Exception e) {
            // keep last good snapshot; log but don't block startup
        }
    }

    private void loadEnvFallback() {
        if (!Files.exists(envPath)) return;
        try {
            Map<String, String> parsed = CredentialsYamlStore.parse(envPath);
            envCache.putAll(parsed);
        } catch (Exception e) {
            // non-fatal
        }
    }

    private void writeCredentialsFile() {
        try {
            CredentialsYamlStore.write(credentialsPath, managedStore);
        } catch (Exception e) {
            throw new RuntimeException("failed to write credentials file: " + credentialsPath, e);
        }
    }
}
