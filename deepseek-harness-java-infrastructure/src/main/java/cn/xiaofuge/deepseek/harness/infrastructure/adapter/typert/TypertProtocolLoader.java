package cn.xiaofuge.deepseek.harness.infrastructure.adapter.typert;

import cn.xiaofuge.deepseek.harness.domain.typert.model.entity.InvocationDescriptor;
import cn.xiaofuge.deepseek.harness.domain.typert.model.entity.TypertContribution;
import cn.xiaofuge.deepseek.harness.domain.typert.model.valobj.TypertFace;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads typert protocol declarations from JSON files.
 *
 * <p>JSON format:
 * <pre>
 * {
 *   "package": "my-plugin",
 *   "face": "host",
 *   "invocations": [
 *     { "namespace": "myPlugin", "method": "doWork",
 *       "paramsSchema": {...}, "resultSchema": {...}, "description": "..." }
 *   ],
 *   "schemas": [{"name": "MyInput", "schema": {...}}]
 * }
 * </pre>
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class TypertProtocolLoader {

    private final ObjectMapper mapper;

    /**
     * 处理Typert Protocol Loader相关逻辑。
     */
    public TypertProtocolLoader(ObjectMapper mapper) {
        this.mapper = mapper;
    }
    /**
     * 加载。
     */

    public TypertContribution load(Path protocolFile) throws IOException {
        JsonNode root = mapper.readTree(Files.readString(protocolFile));
        String packageName = root.path("package").asText();
        TypertFace face = TypertFace.valueOf(root.path("face").asText("HOST").toUpperCase());

        List<InvocationDescriptor> invocations = new ArrayList<>();
        for (JsonNode invNode : root.path("invocations")) {
            invocations.add(new InvocationDescriptor(
                    invNode.path("namespace").asText(),
                    invNode.path("method").asText(),
                    toMap(invNode.path("paramsSchema")),
                    toMap(invNode.path("resultSchema")),
                    invNode.path("description").asText(null)
            ));
        }

        List<Map.Entry<String, Map<String, Object>>> schemas = new ArrayList<>();
        for (JsonNode schemaNode : root.path("schemas")) {
            schemas.add(Map.entry(
                    schemaNode.path("name").asText(),
                    toMap(schemaNode.path("schema"))
            ));
        }

        return new TypertContribution(packageName, face, schemas, invocations);
    }

    /**
     * 处理Suppress Warnings相关逻辑。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        return mapper.convertValue(node, Map.class);
    }
}
