package cn.walioffice.deepseek.harness.domain.skill.model.entity;

import cn.walioffice.deepseek.harness.domain.skill.model.valobj.SkillInvocationPolicy;
import cn.walioffice.deepseek.harness.domain.skill.model.valobj.SkillSource;
import java.util.Optional;

/**
 * Provider catalog entry used by the registry to merge and later load skills.
 *
 * @param summary    skill summary metadata
 * @param rank       lower ranks win duplicate skill names
 * @param locator    opaque provider-owned handle passed back to provider.get()
 * @param path       absolute file path when the provider has one
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SkillCandidate(
        String name,
        String description,
        Optional<String> whenToUse,
        SkillInvocationPolicy invocation,
        SkillSource source,
        String provider,
        Optional<String> path,
        int rank,
        String locator
) {}
