package cn.xiaofuge.deepseek.harness.domain.lsp;

import cn.xiaofuge.deepseek.harness.domain.lsp.model.valobj.LspQueryResult;
import cn.xiaofuge.deepseek.harness.domain.lsp.model.valobj.LspQueryType;
import java.util.Optional;

/**
 * Application-facing LSP seam. Routes queries to the appropriate provider by
 * file extension.
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ILspService {

    /**
     * 处理query。
     * <p>流程：校验入参 → 执行领域动作 → 返回处理结果。</p>
     * @param queryType queryType类型。
     * @param fileContent fileContent内容。
     * @param character character。
     * @return 领域处理结果。
     */
    Optional<LspQueryResult> query(LspQueryType queryType, String filePath, String fileContent, int line, int character);
}
