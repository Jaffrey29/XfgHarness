package cn.walioffice.deepseek.harness.domain.lsp.model.valobj;

/**
 * One LSP location result.
 *
 * @param uri      file URI
 * @param startLine   0-based start line
 * @param startCol    0-based start character
 * @param endLine     0-based end line
 * @param endCol      0-based end character
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record LspLocation(String uri, int startLine, int startCol, int endLine, int endCol) {}
