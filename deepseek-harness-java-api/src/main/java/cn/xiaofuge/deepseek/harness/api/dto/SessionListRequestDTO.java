package cn.xiaofuge.deepseek.harness.api.dto;

public record SessionListRequestDTO(
        String workspaceId,
        int offset,
        int limit
) {
}
