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
     * Query at a position in a file.
     *
     * @param queryType   type of query
     * @param filePath    absolute file path
     * @param fileContent file content
     * @param line        0-based line
     * @param character   0-based character
     * @return query result, or empty if no provider handles the file
     */
    Optional<LspQueryResult> query(LspQueryType queryType, String filePath, String fileContent, int line, int character);
}
