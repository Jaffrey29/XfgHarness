package cn.xiaofuge.deepseek.harness.trigger.service.stream;

import java.util.List;
import java.util.Map;

/**
 * SSE done 事件载荷瘦身工具。
 *
 * <p>背景：长对话场景下 done 事件曾携带全量会话历史（messages 随轮次线性膨胀，
 * 40 轮 × 12KB 时单条 done 可达数百 KB），而完整历史已有
 * {@code GET /api/harness/console/sessions/{id}/messages} 查询接口兜底，
 * 前端也只把 done.messages 当作对账参考而非唯一数据源。</p>
 *
 * <p>瘦身策略：done 只回传「当前回合」的消息（自最后一条 user 消息起，含该 user 消息
 * 到回合结束的全部内容），并附带 {@code messageCount} 会话元信息供客户端判断历史规模。</p>
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public final class DonePayloads {

    private DonePayloads() {
    }

    /**
     * 截取当前回合消息：最后一条 user 消息（含）到列表末尾。
     * 无法识别回合边界（无 user 消息、user 即首条）时原样返回，保持向后兼容。
     */
    public static List<Map<String, Object>> currentTurnMessages(List<Map<String, Object>> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        int lastUserIndex = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(String.valueOf(messages.get(i).get("role")))) {
                lastUserIndex = i;
                break;
            }
        }
        if (lastUserIndex <= 0) {
            return messages;
        }
        return messages.subList(lastUserIndex, messages.size());
    }

    /** 会话消息总数（done 元信息，替代全量历史供客户端判断规模） */
    public static int messageCount(List<Map<String, Object>> messages) {
        return messages == null ? 0 : messages.size();
    }
}
