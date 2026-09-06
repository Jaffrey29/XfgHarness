package cn.walioffice.deepseek.harness.domain.typert.model.entity;

import java.util.Map;

/**
 * Describes one Remote invocation endpoint: a namespace/method pair with
 * JSON-schema-typed parameters and result.
 *
 * @param namespace       wire namespace before the endpoint slash
 * @param method          wire method name after the slash
 * @param paramsSchema    JSON schema for parameters (nullable)
 * @param resultSchema    JSON schema for result (nullable)
 * @param description     human-readable description
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record InvocationDescriptor(
        String namespace,
        String method,
        Map<String, Object> paramsSchema,
        Map<String, Object> resultSchema,
        String description
) {
    public String endpoint() {
        return namespace + "/" + method;
    }
}
