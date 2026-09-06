package cn.walioffice.deepseek.harness.domain.session.log.adapter.repository;

import cn.walioffice.deepseek.harness.domain.session.log.model.entity.SessionEventEntity;
import java.util.List;

/**
 * Defines durable append-only access for session events.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISessionEventRepository {

    void append(SessionEventEntity event);

    List<SessionEventEntity> queryBySessionId(String sessionId);
}
