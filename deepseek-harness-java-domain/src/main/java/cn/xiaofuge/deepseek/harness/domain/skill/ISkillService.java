package cn.xiaofuge.deepseek.harness.domain.skill;

import cn.xiaofuge.deepseek.harness.domain.skill.model.entity.SkillDefinition;
import cn.xiaofuge.deepseek.harness.domain.skill.model.entity.SkillSummary;
import java.util.List;
import java.util.Optional;

/**
 * Application-facing skill seam. Merges provider catalogs, resolves the winning
 * skill for a name, and exposes summaries and definitions to consumers.
 *
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
