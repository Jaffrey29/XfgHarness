package cn.walioffice.deepseek.harness.infrastructure.adapter.repository;

import cn.walioffice.deepseek.harness.domain.session.log.adapter.repository.ISessionEventRepository;
import cn.walioffice.deepseek.harness.domain.session.log.model.entity.SessionEventEntity;
import cn.walioffice.deepseek.harness.infrastructure.dao.ISessionEventDao;
import cn.walioffice.deepseek.harness.infrastructure.dao.po.SessionEventPO;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 通过 DAO 层持久化追加写的会话事件。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Repository
public class SessionEventRepository implements ISessionEventRepository {

    private final ISessionEventDao sessionEventDao;

    public SessionEventRepository(ISessionEventDao sessionEventDao) {
        this.sessionEventDao = sessionEventDao;
    }

    @Override
    public void append(SessionEventEntity event) {
        sessionEventDao.append(new SessionEventPO(
                event.sessionId(),
                event.eventType(),
                event.payload(),
                event.occurredAt()
        ));
    }

    @Override
    public List<SessionEventEntity> queryBySessionId(String sessionId) {
        return sessionEventDao.queryBySessionId(sessionId).stream()
                .map(eventPO -> new SessionEventEntity(
                        eventPO.sessionId(),
                        eventPO.eventType(),
                        eventPO.payload(),
                        eventPO.occurredAt()
                ))
                .toList();
    }
}
