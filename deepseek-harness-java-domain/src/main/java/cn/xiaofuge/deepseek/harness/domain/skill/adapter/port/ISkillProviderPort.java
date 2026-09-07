package cn.xiaofuge.deepseek.harness.domain.skill.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.skill.model.entity.SkillCandidate;
import cn.xiaofuge.deepseek.harness.domain.skill.model.entity.SkillDefinition;
import java.util.List;
import java.util.Optional;

/**
 * Port implemented by skill providers (e.g. filesystem provider).
 * Providers decide where skills come from; the registry merges catalogs.
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
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
