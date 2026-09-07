package cn.xiaofuge.deepseek.harness.domain.tool.web.adapter.port;

import java.util.List;

/**
 * Web port for search and fetch operations.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IWebPort {

    SearchResult search(String query, int maxResults);

    FetchResult fetch(String url, int maxChars);

    /**
     * SearchResult 定义当前模块中的record契约与职责。
     */
    record SearchResult(
            String content,
            List<WebSource> sources,
            boolean truncated
    ) {}

    /**
     * FetchResult 定义当前模块中的record契约与职责。
     */
    record FetchResult(
            String url,
            String content,
            String title,
            String mimeType
    ) {}

    /**
     * WebSource 定义当前模块中的record契约与职责。
     */
    record WebSource(
            String url,
            String title,
            String snippet,
            String publishedAt
    ) {}
}
