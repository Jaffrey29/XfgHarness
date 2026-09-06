package cn.xiaofuge.deepseek.harness.domain.terminal;

import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalSendResultVO;
import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalSessionSnapshotVO;

/**
 * 终端命令服务接口。
 * <p>
 * 执行流程：
 * 1. 上层提交打开、发送、关闭命令；
 * 2. 领域层校验参数；
 * 3. 代理到底层终端端口执行。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ITerminalCommandService {

    TerminalSessionSnapshotVO open(String name, String cwd);

    TerminalSendResultVO send(String sessionId, String command, long timeoutMs);

    TerminalSessionSnapshotVO close(String sessionId);
}
