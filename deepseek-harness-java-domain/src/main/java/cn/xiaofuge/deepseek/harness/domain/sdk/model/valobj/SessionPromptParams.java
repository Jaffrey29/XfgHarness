package cn.xiaofuge.deepseek.harness.domain.sdk.model.valobj;

import cn.xiaofuge.deepseek.harness.domain.model.valobj.ContentBlock;
import java.util.List;

/**
 * One user turn on one SDK session.
 *
 * @param sessionId     the SDK-side session id
 * @param contentBlocks the prompt content blocks, sent verbatim as the user message
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SessionPromptParams(String sessionId, List<ContentBlock> contentBlocks) {}
