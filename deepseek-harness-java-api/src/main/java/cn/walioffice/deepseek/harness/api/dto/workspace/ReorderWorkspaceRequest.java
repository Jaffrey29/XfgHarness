package cn.walioffice.deepseek.harness.api.dto.workspace;

import java.util.List;

/** 承载工作区期望排序。 */
public record ReorderWorkspaceRequest(List<String> order) {
}
