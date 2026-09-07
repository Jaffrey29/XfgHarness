package cn.xiaofuge.deepseek.harness.runtime.agent;

import cn.xiaofuge.deepseek.harness.domain.session.event.adapter.repository.ISessionEventStore;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionEvent;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionHeader;
import cn.xiaofuge.deepseek.harness.domain.session.event.model.entity.SessionLog;
import java.util.List;

/**
 * 持久化会话日志——在内存 WAL 基础上镜像写入持久化事件存储。
 * <p>
 * ReactLoopAgent 调用 {@code session.append(event)} 写入内存 WAL，
 * 本类覆写 append/appendBatch，在内存追加完成后将事件同步镜像到
 * {@link ISessionEventStore}，让事件溯源设施真正运转。
 * <p>
 * 该类位于 infrastructure 层，domain 层的 SessionLog 保持纯净
 * （仅依赖 types SPI，零 Spring 注解）。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class PersistingSessionLog extends SessionLog {

    private final ISessionEventStore store;

    /**
     * 构造持久化会话日志。
     * <p>
     * 同时将 header 持久化到事件存储，确保会话创建即可回放。
     *
     * @param sessionId 会话 ID
     * @param header    会话头
     * @param store     持久化事件存储端口
     */
    public PersistingSessionLog(String sessionId, SessionHeader header, ISessionEventStore store) {
        super(sessionId, header);
        this.store = store;
        // 持久化会话头，让事件溯源从创建即可回放
        store.saveHeader(header);
    }

    @Override
    public synchronized SessionEvent append(SessionEvent event) {
        SessionEvent appended = super.append(event);
        store.append(sessionId(), appended);
        return appended;
    }
    /**
     * 追加Batch。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @param batch batch参数。
     * @return 方法执行结果。
     */


    @Override
    public synchronized List<SessionEvent> appendBatch(List<SessionEvent> batch) {
        List<SessionEvent> appended = super.appendBatch(batch);
        store.appendBatch(sessionId(), appended);
        return appended;
    }
}
