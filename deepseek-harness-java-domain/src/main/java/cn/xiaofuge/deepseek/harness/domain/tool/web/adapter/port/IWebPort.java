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

    record SearchResult(
            String content,
            List<WebSource> sources,
            boolean truncated
    ) {}

    record FetchResult(
            String url,
            String content,
            String title,
            String mimeType
    ) {}

    record WebSource(
            String url,
            String title,
            String snippet,
            String publishedAt
    ) {}
}
