package cn.xiaofuge.deepseek.harness.cases.session.restore;

import cn.xiaofuge.deepseek.harness.api.dto.SessionRestoreRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SessionRestoreResponseDTO;

import cn.xiaofuge.deepseek.harness.cases.session.ISessionRestoreCase;
import cn.xiaofuge.deepseek.harness.cases.session.factory.SessionPipelineFactory;
import org.springframework.stereotype.Service;

/**
 * 薄委托到 {@link SessionRebuilderService}。
 *
 * <p>调用流程：Controller/API → 会话用例 → SessionQueryService/SessionRebuilderService → SessionEventStore。</p>
 * <p>示例：按 sessionId 查询事件后，可重建完整消息时间线。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class SessionRestoreCaseImpl implements ISessionRestoreCase {

    private final SessionPipelineFactory sessionPipelineFactory;

    /**
     * 处理会话 Restore Case Impl。
     * 流程：读取命令 → 执行用例编排 → 返回结果。
     * @param sessionRebuilderService 会话 Rebuilder Service参数。
     * @return 方法执行结果。
     */
    public SessionRestoreCaseImpl(SessionPipelineFactory sessionPipelineFactory) {
        this.sessionPipelineFactory = sessionPipelineFactory;
    }

    @Override
    public SessionRestoreResponseDTO execute(SessionRestoreRequestDTO request) {
        return sessionPipelineFactory.restorePipeline().execute(request);
    }
}
