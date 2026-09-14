package cn.xiaofuge.deepseek.harness.domain.runtime.meter.model.valobj;

import cn.xiaofuge.deepseek.harness.domain.llm.model.valobj.TokenUsage;

public record TokenUsageSummaryVO(
        String sessionId,
        long modelCalls,
        TokenUsage total
) {
}
