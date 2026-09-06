package cn.walioffice.deepseek.harness.infrastructure.adapter.repository;

import cn.walioffice.deepseek.harness.domain.session.registry.adapter.repository.IHarnessSessionRepository;
import cn.walioffice.deepseek.harness.domain.session.registry.model.aggregate.HarnessSessionAggregate;
import cn.walioffice.deepseek.harness.domain.shared.model.valobj.HarnessStatusEnumVO;
import cn.walioffice.deepseek.harness.infrastructure.dao.IHarnessSessionDao;
import cn.walioffice.deepseek.harness.infrastructure.dao.po.HarnessSessionPO;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 通过 DAO 和缓存层持久化会话。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Repository
public class HarnessSessionRepository implements IHarnessSessionRepository {

    private final IHarnessSessionDao harnessSessionDao;

    public HarnessSessionRepository(IHarnessSessionDao harnessSessionDao) {
        this.harnessSessionDao = harnessSessionDao;
    }

    @Override
    public void save(HarnessSessionAggregate session) {
        HarnessSessionPO sessionPO = new HarnessSessionPO(
                session.getSessionId(),
                session.getWorkspaceId(),
                session.getProfileCode(),
                session.getExecutionMode(),
                session.getResolvedModel(),
                session.getStatus().name(),
                session.getCreatedAt()
        );
        harnessSessionDao.save(sessionPO);
    }

    @Override
    public Optional<HarnessSessionAggregate> findBySessionId(String sessionId) {
        return harnessSessionDao.findBySessionId(sessionId).map(this::toAggregate);
    }

    @Override
    public List<HarnessSessionAggregate> listRecent(int limit) {
        return harnessSessionDao.listRecent(limit).stream().map(this::toAggregate).toList();
    }

    private HarnessSessionAggregate toAggregate(HarnessSessionPO sessionPO) {
        HarnessSessionAggregate session = new HarnessSessionAggregate(
                sessionPO.sessionId(),
                sessionPO.workspaceId(),
                sessionPO.profileCode(),
                sessionPO.createdAt()
        );
        session.prepare(sessionPO.executionMode(), sessionPO.resolvedModel());
        session.changeStatus(HarnessStatusEnumVO.valueOf(sessionPO.status()));
        return session;
    }
}
