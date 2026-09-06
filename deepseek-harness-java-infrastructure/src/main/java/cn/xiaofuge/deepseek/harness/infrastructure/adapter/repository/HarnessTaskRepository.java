package cn.xiaofuge.deepseek.harness.infrastructure.adapter.repository;

import cn.xiaofuge.deepseek.harness.domain.shared.model.valobj.HarnessStatusEnumVO;
import cn.xiaofuge.deepseek.harness.domain.task.queue.adapter.repository.IHarnessTaskQueueRepository;
import cn.xiaofuge.deepseek.harness.domain.task.queue.model.entity.HarnessTaskEntity;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.IHarnessTaskDao;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.HarnessTaskPO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 通过 DAO 和缓存层持久化任务。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Repository
public class HarnessTaskRepository implements IHarnessTaskQueueRepository {

    private final IHarnessTaskDao harnessTaskDao;
    private final ObjectMapper objectMapper;

    public HarnessTaskRepository(IHarnessTaskDao harnessTaskDao, ObjectMapper objectMapper) {
        this.harnessTaskDao = harnessTaskDao;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(HarnessTaskEntity task) {
        HarnessTaskPO taskPO = new HarnessTaskPO(
                task.getTaskId(),
                task.getSessionId(),
                task.getPrompt(),
                toJson(task.getEnabledTools()),
                task.getStatus().name(),
                task.getSubmittedAt()
        );
        harnessTaskDao.save(taskPO);
    }

    @Override
    public Optional<HarnessTaskEntity> findByTaskId(String taskId) {
        return harnessTaskDao.findByTaskId(taskId).map(taskPO -> new HarnessTaskEntity(
                taskPO.taskId(),
                taskPO.sessionId(),
                taskPO.prompt(),
                fromJson(taskPO.enabledToolsJson()),
                HarnessStatusEnumVO.valueOf(taskPO.status()),
                taskPO.submittedAt()
        ));
    }

    private String toJson(List<String> enabledTools) {
        try {
            return objectMapper.writeValueAsString(enabledTools);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize enabled tools", exception);
        }
    }

    private List<String> fromJson(String enabledToolsJson) {
        try {
            return objectMapper.readValue(enabledToolsJson, new TypeReference<>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to deserialize enabled tools", exception);
        }
    }
}
