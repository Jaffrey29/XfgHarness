package cn.walioffice.deepseek.harness.domain.skill.model.entity;

import cn.walioffice.deepseek.harness.domain.skill.model.valobj.SkillInvocationPolicy;
import cn.walioffice.deepseek.harness.domain.skill.model.valobj.SkillSource;
import java.util.Optional;

/**
 * Invocation-neutral skill metadata returned by {@code list()}.
 *
 * @param name        kebab-case identifier
 * @param description short routing description
 * @param whenToUse   optional extra routing guidance
 * @param invocation  resolved invocation controls
 * @param source      discovery source
 * @param provider    provider that owns this skill body
 * @param path        absolute file path when the skill came from disk
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SkillSummary(
        String name,
        String description,
        Optional<String> whenToUse,
        SkillInvocationPolicy invocation,
        SkillSource source,
        String provider,
        Optional<String> path
) {}
