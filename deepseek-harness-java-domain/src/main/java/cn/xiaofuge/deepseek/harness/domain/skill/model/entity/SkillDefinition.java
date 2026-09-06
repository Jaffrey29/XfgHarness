package cn.xiaofuge.deepseek.harness.domain.skill.model.entity;

import cn.xiaofuge.deepseek.harness.domain.skill.model.valobj.SkillInvocationPolicy;
import cn.xiaofuge.deepseek.harness.domain.skill.model.valobj.SkillSource;
import java.util.Optional;

/**
 * Complete parsed skill definition, including the body loaded by {@code get()}.
 *
 * @param name        kebab-case identifier
 * @param description short routing description
 * @param whenToUse   optional extra routing guidance
 * @param invocation  resolved invocation controls
 * @param source      discovery source
 * @param provider    provider that owns this skill body
 * @param content     Markdown instruction body
 * @param path        absolute file path when from disk
 *
 * @author 小傅哥
 * @website bugstack.cn
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
