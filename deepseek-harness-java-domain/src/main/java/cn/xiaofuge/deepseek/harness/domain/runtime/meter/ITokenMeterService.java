package cn.xiaofuge.deepseek.harness.domain.runtime.meter;

import cn.xiaofuge.deepseek.harness.domain.runtime.meter.model.valobj.TokenUsageSummaryVO;

public interface ITokenMeterService {

    TokenUsageSummaryVO summarize(String sessionId);
}
