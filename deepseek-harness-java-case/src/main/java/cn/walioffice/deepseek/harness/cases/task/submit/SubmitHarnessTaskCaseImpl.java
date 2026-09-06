package cn.walioffice.deepseek.harness.cases.task.submit;

import cn.walioffice.deepseek.harness.api.dto.task.SubmitTaskRequest;
import cn.walioffice.deepseek.harness.api.dto.task.SubmitTaskResponse;
import cn.walioffice.deepseek.harness.cases.task.submit.factory.SubmitHarnessTaskFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 轻量入口：通过工厂委托给策略树处理。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class SubmitHarnessTaskCaseImpl implements ISubmitHarnessTaskCase {

    @Autowired
    private SubmitHarnessTaskFactory factory;

    @Override
    public SubmitTaskResponse execute(SubmitTaskRequest request) {
        try {
            return factory.strategyHandler().apply(request, factory.newContext());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
