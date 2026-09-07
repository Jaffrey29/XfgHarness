package cn.xiaofuge.deepseek.harness.domain.skill;

import cn.xiaofuge.deepseek.harness.domain.skill.model.entity.SkillDefinition;
import cn.xiaofuge.deepseek.harness.domain.skill.model.entity.SkillSummary;
import java.util.List;
import java.util.Optional;

/**
 * Application-facing skill seam. Merges provider catalogs, resolves the winning
 * skill for a name, and exposes summaries and definitions to consumers.
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISkillService {

    /**
     * List all winning skill summaries.
     *
     * @param cwd workspace selector
     * @return skill summaries (one per unique name, lowest rank wins)
     */
    List<SkillSummary> list(String cwd);

    /**
     * Load the full definition for one skill by name.
     *
     * @param name skill name (kebab-case)
     * @param cwd  workspace selector
     * @return the loaded skill definition, or empty if not found
     */
    Optional<SkillDefinition> get(String name, String cwd);
}
