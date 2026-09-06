package cn.xiaofuge.deepseek.harness.domain.skill.service;

import cn.xiaofuge.deepseek.harness.domain.skill.ISkillService;
import cn.xiaofuge.deepseek.harness.domain.skill.adapter.port.ISkillProviderPort;
import cn.xiaofuge.deepseek.harness.domain.skill.model.entity.SkillCandidate;
import cn.xiaofuge.deepseek.harness.domain.skill.model.entity.SkillDefinition;
import cn.xiaofuge.deepseek.harness.domain.skill.model.entity.SkillSummary;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Skill registry: merges provider catalogs, resolves the winning skill for a
 * name (lowest rank wins), and exposes summaries and definitions.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class SkillService implements ISkillService {

    private static final Pattern SKILL_NAME = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    private final List<ISkillProviderPort> providers;
    private final Map<String, List<SkillCandidate>> cache = new ConcurrentHashMap<>();

    public SkillService(List<ISkillProviderPort> providers) {
        this.providers = providers;
    }

    @Override
    public List<SkillSummary> list(String cwd) {
        List<SkillCandidate> all = discoverAll(cwd);
        // Merge: lowest rank wins per name
        Map<String, SkillCandidate> winners = new TreeMap<>();
        for (SkillCandidate c : all) {
            SkillCandidate existing = winners.get(c.name());
            if (existing == null || c.rank() < existing.rank()) {
                winners.put(c.name(), c);
            }
        }
        return winners.values().stream()
                .map(c -> new SkillSummary(c.name(), c.description(), c.whenToUse(),
                        c.invocation(), c.source(), c.provider(), c.path()))
                .toList();
    }

    @Override
    public Optional<SkillDefinition> get(String name, String cwd) {
        if (name == null || !SKILL_NAME.matcher(name).matches()) return Optional.empty();
        List<SkillCandidate> all = discoverAll(cwd);
        // Find the winning candidate for this name
        SkillCandidate winner = null;
        for (SkillCandidate c : all) {
            if (c.name().equals(name) && (winner == null || c.rank() < winner.rank())) {
                winner = c;
            }
        }
        if (winner == null) return Optional.empty();
        // Find the provider that produced this candidate
        for (ISkillProviderPort provider : providers) {
            Optional<SkillDefinition> def = provider.load(winner);
            if (def.isPresent()) return def;
        }
        return Optional.empty();
    }

    private List<SkillCandidate> discoverAll(String cwd) {
        List<SkillCandidate> all = new ArrayList<>();
        for (ISkillProviderPort provider : providers) {
            all.addAll(provider.discover(cwd));
        }
        return all;
    }
}
