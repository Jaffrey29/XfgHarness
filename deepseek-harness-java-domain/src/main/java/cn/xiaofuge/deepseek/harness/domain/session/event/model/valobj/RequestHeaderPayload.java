package cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj;
import java.util.List;
import java.util.Map;
public record RequestHeaderPayload(Map<String, Object> config, Map<String, Object> adapterDefaults, String system, List<Map<String, Object>> tools) {}
