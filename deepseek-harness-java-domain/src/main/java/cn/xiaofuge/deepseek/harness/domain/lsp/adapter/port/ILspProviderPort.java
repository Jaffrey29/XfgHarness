package cn.xiaofuge.deepseek.harness.domain.lsp.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.lsp.model.valobj.LspQueryResult;
import cn.xiaofuge.deepseek.harness.domain.lsp.model.valobj.LspQueryType;

/**
 * Port for one LSP provider that serves transient-open queries.
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ILspProviderPort {

    /**
     * Query the language server at a position in a file.
     *
     * @param queryType   type of query
     * @param filePath    absolute file path
     * @param fileContent file content (transient-open)
     * @param line        0-based line
     * @param character   0-based character
     * @return query result
     */
    LspQueryResult query(LspQueryType queryType, String filePath, String fileContent, int line, int character);
}
