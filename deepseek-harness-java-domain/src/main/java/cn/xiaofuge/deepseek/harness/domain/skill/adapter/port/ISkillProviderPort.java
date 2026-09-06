package cn.xiaofuge.deepseek.harness.domain.skill.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.skill.model.entity.SkillCandidate;
import cn.xiaofuge.deepseek.harness.domain.skill.model.entity.SkillDefinition;
import java.util.List;
import java.util.Optional;

/**
 * Port implemented by skill providers (e.g. filesystem provider).
 * Providers decide where skills come from; the registry merges catalogs.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISkillProviderPort {

    /**
     * Discover all skill candidates from this provider's sources.
     *
     * @param cwd workspace selector for the current lookup
     * @return list of skill candidates
     */
    List<SkillCandidate> discover(String cwd);

    /**
     * Load the full skill definition for one candidate.
     *
     * @param candidate the candidate to load
     * @return the loaded skill definition, or empty if not found
     */
    Optional<SkillDefinition> load(SkillCandidate candidate);
}
