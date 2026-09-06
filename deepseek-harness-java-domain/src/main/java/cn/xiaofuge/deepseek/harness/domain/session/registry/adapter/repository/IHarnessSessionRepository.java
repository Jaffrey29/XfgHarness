package cn.xiaofuge.deepseek.harness.domain.session.registry.adapter.repository;

import cn.xiaofuge.deepseek.harness.domain.session.registry.model.aggregate.HarnessSessionAggregate;
import java.util.List;
import java.util.Optional;

/**
 * Defines durable access for session aggregates.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IHarnessSessionRepository {

    void save(HarnessSessionAggregate session);

    Optional<HarnessSessionAggregate> findBySessionId(String sessionId);

    List<HarnessSessionAggregate> listRecent(int limit);
}
