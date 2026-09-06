package cn.xiaofuge.deepseek.harness.infrastructure.adapter.credentials;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal YAML key=value store for credential files — no external dependency.
 * Format: {@code KEY: value} per line, blank lines and comments ignored.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
final class CredentialsYamlStore {

    private CredentialsYamlStore() {}

    static Map<String, String> parse(Path path) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        for (String line : Files.readAllLines(path)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            int sep = trimmed.indexOf(':');
            if (sep <= 0) continue;
            String key = trimmed.substring(0, sep).trim();
            String val = trimmed.substring(sep + 1).trim();
            if (!key.isEmpty()) result.put(key, val);
        }
        return result;
    }

    static void write(Path path, Map<String, String> store) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# DeepSeek Harness credentials\n");
        sb.append("# Writable via the credentials seam. Do not edit manually unless necessary.\n");
        for (Map.Entry<String, String> entry : store.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        Files.writeString(path, sb);
        // 0600: owner-only
        Files.setPosixFilePermissions(path, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
    }
}
