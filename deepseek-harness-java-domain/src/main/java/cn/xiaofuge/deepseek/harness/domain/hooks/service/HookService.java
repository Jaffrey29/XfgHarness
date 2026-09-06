package cn.xiaofuge.deepseek.harness.domain.hooks.service;
import java.util.concurrent.ConcurrentHashMap;


import cn.xiaofuge.deepseek.harness.domain.hooks.IHookService;
import cn.xiaofuge.deepseek.harness.domain.hooks.adapter.port.IHookRunnerPort;
import cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj.*;

import java.util.*;

/**
 * Hook service: matches configured hooks against a hook point and tool name,
 * runs them via the runner port, and merges outcomes.
 *
 * <p>Configuration is a map of hook point → list of matcher groups, per dialect.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class HookService implements IHookService {

    private final IHookRunnerPort runnerPort;
    private final Map<HookDialect, Map<HookPoint, List<MatcherGroup>>> config = new ConcurrentHashMap<>();

    public HookService(IHookRunnerPort runnerPort) {
        this.runnerPort = runnerPort;
    }

    public void registerMatchers(HookDialect dialect, HookPoint point, List<MatcherGroup> groups) {
        config.computeIfAbsent(dialect, d -> new EnumMap<>(HookPoint.class))
                .put(point, List.copyOf(groups));
    }

    @Override
    public MergedHookOutcome runHooks(HookDialect dialect, HookPoint point, String toolName,
                                      String payloadJson, Map<String, String> environment) {
        Map<HookPoint, List<MatcherGroup>> dialectConfig = config.get(dialect);
        if (dialectConfig == null) return emptyOutcome();
        List<MatcherGroup> groups = dialectConfig.get(point);
        if (groups == null || groups.isEmpty()) return emptyOutcome();

        List<MatcherGroup> matching = HookMatcher.filterMatching(dialect, groups, toolName);
        if (matching.isEmpty()) return emptyOutcome();

        List<HookOutput> outputs = new ArrayList<>();
        for (MatcherGroup group : matching) {
            for (CommandHook hook : group.hooks()) {
                HookOutput output = runnerPort.run(hook, payloadJson, environment);
                outputs.add(output);
            }
        }
        return HookOutputMerger.merge(outputs);
    }

    private static MergedHookOutcome emptyOutcome() {
        return new MergedHookOutcome(Optional.empty(), "", "", false, Optional.empty());
    }
}
