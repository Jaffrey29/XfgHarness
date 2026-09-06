package cn.xiaofuge.deepseek.harness.domain.coderuntime;

import cn.xiaofuge.deepseek.harness.domain.coderuntime.model.valobj.CodeRunRequest;
import cn.xiaofuge.deepseek.harness.domain.coderuntime.model.valobj.CodeRunResult;

/**
 * Application-facing code-execution seam.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ICodeRuntimeService {
    CodeRunResult execute(CodeRunRequest request);
}
