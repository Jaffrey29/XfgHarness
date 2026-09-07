package cn.xiaofuge.deepseek.harness.domain.skill.model.entity;

import cn.xiaofuge.deepseek.harness.domain.skill.model.valobj.SkillInvocationPolicy;
import cn.xiaofuge.deepseek.harness.domain.skill.model.valobj.SkillSource;
import java.util.Optional;

/**
 * SkillDefinition 表示SkillDefinition。
 */
public record SkillDefinition(
        String name,
        String description,
        Optional<String> whenToUse,
        SkillInvocationPolicy invocation,
        SkillSource source,
        String provider,
        String content,
        Optional<String> path
) {}
