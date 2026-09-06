package cn.walioffice.deepseek.harness.domain.skill.model.valobj;

/**
 * Invocation controls shared by skill discovery consumers.
 *
 * @param modelInvocable whether model-facing catalogs include this skill
 * @param userInvocable  whether human-facing command catalogs include this skill
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SkillInvocationPolicy(boolean modelInvocable, boolean userInvocable) {
    public static SkillInvocationPolicy both() { return new SkillInvocationPolicy(true, true); }
}
