package cn.walioffice.deepseek.harness.api.dto.terminal;

/**
 * 终端打开请求。
 * <p>
 * 执行流程：
 * 1. 前端提交名称与工作目录；
 * 2. API 层转交领域服务；
 * 3. 基础设施层启动 shell 进程。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record TerminalOpenRequest(String name, String cwd) {
}
