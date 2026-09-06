package cn.walioffice.deepseek.harness.domain.session.registry.model.valobj;

import cn.walioffice.deepseek.harness.domain.shared.model.valobj.HarnessStatusEnumVO;
import java.time.Instant;

/**
 * Returns the result of opening one session.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SessionOpenedResultVO(
        String sessionId,
        String resolvedModel,
        HarnessStatusEnumVO status,
        Instant acceptedAt
) {
}
