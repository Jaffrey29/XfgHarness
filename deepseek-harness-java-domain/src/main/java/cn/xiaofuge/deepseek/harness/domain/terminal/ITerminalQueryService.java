package cn.xiaofuge.deepseek.harness.domain.terminal;

import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalReadResultVO;
import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalSessionSnapshotVO;
import java.util.List;

/**
 * 终端查询服务接口。
 * <p>
 * 执行流程：
 * 1. 上层请求终端状态或输出；
 * 2. 领域层读取端口快照；
 * 3. 返回只读结果。
 *
 * <p>调用流程：打开会话 → 发送命令 → 读取输出 → 关闭会话。</p>
 * <p>示例：发送 pwd 后读取标准输出，超时后可关闭终端会话。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ITerminalQueryService {

    TerminalReadResultVO read(String sessionId, int maxChars);

    List<TerminalSessionSnapshotVO> list();
}
