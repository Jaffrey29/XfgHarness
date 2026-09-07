package cn.xiaofuge.deepseek.harness.infrastructure.adapter.repository;

import cn.xiaofuge.deepseek.harness.domain.goal.adapter.repository.IGoalRepository;
import cn.xiaofuge.deepseek.harness.domain.goal.model.entity.GoalAggregate;
import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalStatus;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.IGoalStateDao;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.GoalStatePO;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 目标仓储实现。
 * <p>
 * 执行流程：
 * 1. 接收领域目标聚合；
 * 2. 映射为持久化对象；
 * 3. 通过 DAO 读写数据库。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Repository
public class GoalRepository implements IGoalRepository {

    private final IGoalStateDao goalStateDao;

    /**
     * 处理Goal Repository相关逻辑。
     */
    public GoalRepository(IGoalStateDao goalStateDao) {
        this.goalStateDao = goalStateDao;
    }

    @Override
    public Optional<GoalAggregate> findBySessionId(String sessionId) {
        return goalStateDao.findBySessionId(sessionId).map(this::toAggregate);
    }

    @Override
    @Transactional
    public void save(GoalAggregate aggregate) {
        goalStateDao.save(new GoalStatePO(
                aggregate.sessionId(),
                aggregate.goalId(),
                aggregate.revision(),
                aggregate.title(),
                aggregate.description(),
                aggregate.status().name(),
                aggregate.blockedReason(),
                aggregate.maxRounds(),
                aggregate.roundsStarted(),
                aggregate.continuationEnabled(),
                aggregate.createdAt(),
                aggregate.updatedAt()
        ));
    }

    @Override
    public List<GoalAggregate> queryAll() {
        return goalStateDao.queryAll().stream().map(this::toAggregate).toList();
    }

    /**
     * 处理to Aggregate相关逻辑。
     */
    private GoalAggregate toAggregate(GoalStatePO po) {
        return new GoalAggregate(
                po.sessionId(),
                po.goalId(),
                po.revision(),
                po.title(),
                po.description(),
                GoalStatus.valueOf(po.status()),
                po.blockedReason(),
                po.maxRounds(),
                po.roundsStarted(),
                po.continuationEnabled(),
                po.createdAt(),
                po.updatedAt()
        );
    }
}
