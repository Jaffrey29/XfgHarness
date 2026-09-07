package cn.xiaofuge.deepseek.harness.domain.skill.model.valobj;

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
    /**
     * 处理both。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @return 方法执行结果。
     */
    public static SkillInvocationPolicy both() { return new SkillInvocationPolicy(true, true); }
}
