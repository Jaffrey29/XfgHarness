package cn.xiaofuge.deepseek.harness.infrastructure.adapter.extension;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 扩展配置持久化存储：harness.yml 打在 jar 里不可写，运行期通过
 * 本存储把用户在设置页 / 对话里做的扩展变更落到独立 JSON 文件，
 * 启动时与 yml 预置合并（用户配置优先）。
 *
 * <p>文件位置：{@code ${HARNESS_CONFIG_DIR:${user.home}/.dsh}/extensions.json}。</p>
 *
 * <p>调用流程：触发层/工具 → 扩展管理服务 → 本存储 → 本地文件。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public class ExtensionConfigStore {

    /** 单条 MCP server 持久化结构（与 HarnessExtensionsProperties.McpServer 对齐）。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StoredMcpServer(
            String name,
            String transport,
            String command,
            List<String> args,
            Map<String, String> env,
            String cwd,
            String url,
            Map<String, String> headers
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StoredConfig {
        public List<String> disabledSkills = new ArrayList<>();
        public List<StoredMcpServer> mcpServers = new ArrayList<>();
        public Map<String, String> cli = new LinkedHashMap<>();
    }

    private final ObjectMapper objectMapper;
    private final Path file;
    private final Object lock = new Object();
    private volatile StoredConfig cached;

    public ExtensionConfigStore(ObjectMapper objectMapper, Path file) {
        this.objectMapper = objectMapper;
        this.file = file;
    }

    /** 启动时调用：确保文件与缓存就绪；文件损坏时备份后重建。 */
    public void init() {
        synchronized (lock) {
            cached = load();
        }
    }

    public StoredConfig snapshot() {
        StoredConfig config = cached;
        if (config != null) return config;
        synchronized (lock) {
            if (cached == null) cached = load();
            return cached;
        }
    }

    public void update(java.util.function.Consumer<StoredConfig> mutator) {
        synchronized (lock) {
            StoredConfig config = cached != null ? cached : load();
            mutator.accept(config);
            save(config);
            cached = config;
        }
    }

    private StoredConfig load() {
        try {
            if (Files.isRegularFile(file)) {
                return objectMapper.readValue(Files.readString(file), StoredConfig.class);
            }
        } catch (IOException e) {
            // 文件损坏：备份后重建，不阻塞启动
            try {
                Files.copy(file, file.resolveSibling("extensions.json.corrupt"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) { }
        }
        return new StoredConfig();
    }

    private void save(StoredConfig config) {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new IllegalStateException("保存扩展配置失败: " + file + " - " + e.getMessage(), e);
        }
    }

    /** 供诊断：返回配置文件路径。 */
    public Path file() {
        return file;
    }

    /** 防御式拷贝工具：列表 null 安全。 */
    public static <T> List<T> copyList(List<T> list) {
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    /** 防御式拷贝工具：Map null 安全。 */
    public static Map<String, String> copyMap(Map<String, String> map) {
        return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
    }

    /** 递归删除目录（技能卸载用）。 */
    public static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> stream = Files.walk(root)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    public static class UncheckedIOException extends RuntimeException {
        public UncheckedIOException(IOException cause) { super(cause.getMessage(), cause); }
    }
}
