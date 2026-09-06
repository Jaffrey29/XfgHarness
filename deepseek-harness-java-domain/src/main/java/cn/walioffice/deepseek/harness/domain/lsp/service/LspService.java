package cn.walioffice.deepseek.harness.domain.lsp.service;

import cn.walioffice.deepseek.harness.domain.lsp.ILspService;
import cn.walioffice.deepseek.harness.domain.lsp.adapter.port.ILspProviderPort;
import cn.walioffice.deepseek.harness.domain.lsp.model.valobj.LspQueryResult;
import cn.walioffice.deepseek.harness.domain.lsp.model.valobj.LspQueryType;
import java.util.*;

/**
 * LSP service: routes queries to the appropriate provider by file extension.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class LspService implements ILspService {

    private final Map<String, ILspProviderPort> providersByExtension = new HashMap<>();

    public void registerProvider(String fileExtension, ILspProviderPort provider) {
        providersByExtension.put(fileExtension, provider);
    }

    @Override
    public Optional<LspQueryResult> query(LspQueryType queryType, String filePath, String fileContent,
                                          int line, int character) {
        int dot = filePath.lastIndexOf('.');
        if (dot < 0) return Optional.empty();
        String ext = filePath.substring(dot);
        ILspProviderPort provider = providersByExtension.get(ext);
        if (provider == null) return Optional.empty();
        try {
            return Optional.of(provider.query(queryType, filePath, fileContent, line, character));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
