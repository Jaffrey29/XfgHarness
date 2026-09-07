package cn.xiaofuge.deepseek.harness.api.dto;

public record ResolveRuntimeApprovalResponseDTO(boolean resolved, String verdict, String error) {}
