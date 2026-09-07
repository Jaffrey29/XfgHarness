package cn.xiaofuge.deepseek.harness.domain.coderuntime.service;

import cn.xiaofuge.deepseek.harness.domain.coderuntime.ICodeRuntimeService;
import cn.xiaofuge.deepseek.harness.domain.coderuntime.adapter.port.ICodeRuntimePort;
import cn.xiaofuge.deepseek.harness.domain.coderuntime.model.valobj.CodeRunRequest;
import cn.xiaofuge.deepseek.harness.domain.coderuntime.model.valobj.CodeRunResult;
import org.springframework.stereotype.Service;

@Service
/**
 * CodeRuntimeService 定义当前模块中的class契约与职责。
 */
public class CodeRuntimeService implements ICodeRuntimeService {
    private final ICodeRuntimePort port;

    /**
     * 处理Code Runtime Service相关逻辑。
     */
    public CodeRuntimeService(ICodeRuntimePort port) { this.port = port; }

    @Override
    public CodeRunResult execute(CodeRunRequest request) {
        return port.execute(request);
    }
}
