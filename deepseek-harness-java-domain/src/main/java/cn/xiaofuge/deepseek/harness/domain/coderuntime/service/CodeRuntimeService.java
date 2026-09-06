package cn.xiaofuge.deepseek.harness.domain.coderuntime.service;

import cn.xiaofuge.deepseek.harness.domain.coderuntime.ICodeRuntimeService;
import cn.xiaofuge.deepseek.harness.domain.coderuntime.adapter.port.ICodeRuntimePort;
import cn.xiaofuge.deepseek.harness.domain.coderuntime.model.valobj.CodeRunRequest;
import cn.xiaofuge.deepseek.harness.domain.coderuntime.model.valobj.CodeRunResult;

public class CodeRuntimeService implements ICodeRuntimeService {
    private final ICodeRuntimePort port;

    public CodeRuntimeService(ICodeRuntimePort port) { this.port = port; }

    @Override
    public CodeRunResult execute(CodeRunRequest request) {
        return port.execute(request);
    }
}
