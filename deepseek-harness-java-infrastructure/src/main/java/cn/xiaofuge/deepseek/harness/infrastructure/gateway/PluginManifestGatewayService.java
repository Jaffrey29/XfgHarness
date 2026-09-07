package cn.xiaofuge.deepseek.harness.infrastructure.gateway;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Reads plugin manifest files from the local filesystem.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class PluginManifestGatewayService {

    private final ObjectMapper objectMapper;

    /**
     * 处理Plugin Manifest Gateway Service相关逻辑。
     */
    public PluginManifestGatewayService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Reads one JSON file into a generic key/value map when it exists.
     *
     * @param path file path
     * @return parsed manifest map when present
     */
    public Optional<Map<String, Object>> readJson(Path path) {
        if (!Files.exists(path) || Files.isDirectory(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(path.toFile(), new TypeReference<>() { }));
        } catch (IOException exception) {
            throw new IllegalStateException("failed to parse manifest: " + path, exception);
        }
    }
}
