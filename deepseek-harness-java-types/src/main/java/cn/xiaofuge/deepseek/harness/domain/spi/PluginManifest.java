package cn.xiaofuge.deepseek.harness.domain.spi;

import java.util.Objects;

/**
 * {@code META-INF/plugin.yaml} 清单的解析结果。
 * <p>
 * 清单是嵌入插件 JAR 的轻量 YAML 文件：
 * <pre>{@code
 * id: my-plugin
 * name: My Plugin
 * version: 1.0.0
 * author: Jane Doe
 * description: A sample Java harness plugin
 * entrypoint: com.example.MyPluginEntry
 * }</pre>
 * <p>
 * {@code entrypoint} 是实现 {@link JavaHarnessPlugin} 的全限定类名。该字段可选；
 * 未填写时 Harness 会回退到 {@code META-INF/services} 的 ServiceLoader 发现机制。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public final class PluginManifest {

    private final String id;
    private final String name;
    private final String version;
    private final String author;
    private final String description;
    private final String entrypoint;

    public PluginManifest(String id, String name, String version, String author, String description, String entrypoint) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.version = Objects.requireNonNull(version, "version");
        this.author = author;
        this.description = description;
        this.entrypoint = entrypoint;
    }

    public String id() { return id; }
    public String name() { return name; }
    public String version() { return version; }
    public String author() { return author; }
    public String description() { return description; }
    public String entrypoint() { return entrypoint; }
}
