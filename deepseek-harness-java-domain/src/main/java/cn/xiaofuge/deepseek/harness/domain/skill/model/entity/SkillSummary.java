package cn.xiaofuge.deepseek.harness.domain.skill.model.entity;

import cn.xiaofuge.deepseek.harness.domain.skill.model.valobj.SkillInvocationPolicy;
import cn.xiaofuge.deepseek.harness.domain.skill.model.valobj.SkillSource;
import java.util.Optional;

/**
 * SkillSummary 表示SkillSummary。
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
