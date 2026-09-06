package cn.walioffice.deepseek.harness.domain.typert.model.entity;

import cn.walioffice.deepseek.harness.domain.typert.model.valobj.TypertFace;
import java.util.List;
import java.util.Map;

/**
 * One generated package contribution registered and withdrawn atomically.
 *
 * @param packageName  contributing package name
 * @param face         independently compiled face (host or client)
 * @param schemas      list of named schemas (name → JSON schema)
 * @param invocations  host invocation definitions
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record TypertContribution(
        String packageName,
        TypertFace face,
        List<Map.Entry<String, Map<String, Object>>> schemas,
        List<InvocationDescriptor> invocations
) {}
