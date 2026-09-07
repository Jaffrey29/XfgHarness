package cn.xiaofuge.deepseek.harness.domain.typert.model.entity;

import cn.xiaofuge.deepseek.harness.domain.typert.model.valobj.TypertFace;
import java.util.List;
import java.util.Map;

/**
 * TypertContribution 表示TypertContribution。
 */
public record TypertContribution(
        String packageName,
        TypertFace face,
        List<Map.Entry<String, Map<String, Object>>> schemas,
        List<InvocationDescriptor> invocations
) {}
