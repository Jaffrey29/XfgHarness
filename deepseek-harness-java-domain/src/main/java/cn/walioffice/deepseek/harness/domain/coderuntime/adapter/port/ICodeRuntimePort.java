package cn.walioffice.deepseek.harness.domain.coderuntime.adapter.port;

import cn.walioffice.deepseek.harness.domain.coderuntime.model.valobj.CodeRunRequest;
import cn.walioffice.deepseek.harness.domain.coderuntime.model.valobj.CodeRunResult;

/**
 * Port for executing code against host async bindings.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ICodeRuntimePort {

    /**
     * Execute a code program with the given request.
     *
     * @param request the code run request
     * @return execution result
     */
    CodeRunResult execute(CodeRunRequest request);
}
