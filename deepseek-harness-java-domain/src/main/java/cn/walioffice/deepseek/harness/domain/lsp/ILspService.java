package cn.walioffice.deepseek.harness.domain.lsp;

import cn.walioffice.deepseek.harness.domain.lsp.model.valobj.LspQueryResult;
import cn.walioffice.deepseek.harness.domain.lsp.model.valobj.LspQueryType;
import java.util.Optional;

/**
 * Application-facing LSP seam. Routes queries to the appropriate provider by
 * file extension.
 *
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
