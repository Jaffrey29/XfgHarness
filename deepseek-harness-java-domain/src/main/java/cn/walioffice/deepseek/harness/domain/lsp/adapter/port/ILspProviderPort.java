package cn.walioffice.deepseek.harness.domain.lsp.adapter.port;

import cn.walioffice.deepseek.harness.domain.lsp.model.valobj.LspQueryResult;
import cn.walioffice.deepseek.harness.domain.lsp.model.valobj.LspQueryType;

/**
 * Port for one LSP provider that serves transient-open queries.
 *
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
