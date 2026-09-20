package cn.xiaofuge.deepseek.harness.infrastructure.digitalhuman;

import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.llm.adapter.port.ILlmRuntimePort;
import cn.xiaofuge.deepseek.harness.domain.llm.model.valobj.GenerateOptions;
import cn.xiaofuge.deepseek.harness.domain.llm.service.LlmGenerationService;
import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;
import cn.xiaofuge.deepseek.harness.domain.model.entity.MessageSource;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.service.AgentModelSettingResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * LLM 规划器：根据房间目标与参与者能力卡，用模型生成 DAG 分工计划。
 *
 * <p>输出契约是严格的 JSON 数组，解析失败或结构非法时返回 null，
 * 由调用方回退到规则型计划——规划器是增强，不是单点。</p>
 */
@Service
public class PlannerService {

    private static final Logger log = LoggerFactory.getLogger(PlannerService.class);

    private final ILlmRuntimePort llmRuntimePort;
    private final AgentModelSettingResolver modelSettingResolver;
    private final ObjectMapper objectMapper;

    public PlannerService(ILlmRuntimePort llmRuntimePort,
                          AgentModelSettingResolver modelSettingResolver) {
        this.llmRuntimePort = llmRuntimePort;
        this.modelSettingResolver = modelSettingResolver;
        this.objectMapper = new ObjectMapper();
    }

    /** 一步计划 */
    public record PlanStep(String assignTo, String title, String instruction, List<Integer> dependsOn) {
    }

    /** 参与者能力卡（规划输入） */
    public record CapabilityCard(String digitalHumanId, String displayName, String purpose, List<String> roleTags) {
    }

    /**
     * 生成分工计划。
     *
     * @return 计划步骤列表；模型不可用或输出非法时返回 null（调用方回退规则型）
     */
    public List<PlanStep> plan(String objective, List<CapabilityCard> participants) {
        if (participants == null || participants.isEmpty()) return null;
        try {
            AgentOptions agent = modelSettingResolver.resolve(null, 2048);
            String prompt = buildPrompt(objective, participants);
            Message userMessage = Message.createUser(
                    List.of(new TextBlock(prompt)), new MessageSource.UserSource());
            GenerateOptions options = new GenerateOptions(
                    agent.provider(), agent.model(), null,
                    List.of(userMessage),
                    "你是多智能体协作的规划器，只输出严格 JSON，不输出任何解释。",
                    null, 0.2, 2048, null, "planner-" + System.currentTimeMillis(),
                    "collaboration-planning", agent.baseUrl(), agent.apiKey(), agent.protocol());

            Message result = new LlmGenerationService(llmRuntimePort).generate(options).join();
            String text = result.content().stream()
                    .map(block -> block instanceof TextBlock tb ? tb.text() : "")
                    .reduce("", String::concat);
            return parse(text, participants);
        } catch (Exception e) {
            log.warn("LLM planner failed, falling back to rule-based plan: {}", e.getMessage());
            return null;
        }
    }

    private String buildPrompt(String objective, List<CapabilityCard> participants) {
        StringBuilder cards = new StringBuilder();
        for (CapabilityCard card : participants) {
            cards.append("- id=").append(card.digitalHumanId())
                    .append(" 名称=").append(card.displayName())
                    .append(" 职责=").append(card.purpose())
                    .append(" 能力标签=").append(String.join("/", card.roleTags()))
                    .append("\n");
        }
        return """
                你是一个多智能体协作规划器。给定一个目标和一组数字员工，把目标拆成一个有向无环的执行计划。

                【目标】
                %s

                【可用数字员工】
                %s
                【要求】
                1. 每一步明确派给一名员工（assignTo 用其 id），任务要落在该员工的职责与能力标签内。
                2. 有先后依赖的步骤用 dependsOn 引用前序步骤的序号（从 0 开始）；无依赖的步骤可以并行。
                3. 每名数字员工至少分到一步；如果职责可以并行，dependsOn 必须为空数组，避免无意义串行。
                4. 步骤尽量少而精，通常 %d-%d 步，不要为凑数拆分。
                5. title 用一句话概括该步产出，instruction 是给该员工的完整任务说明。
                6. 只输出 JSON 数组，不要输出任何其他文字、解释或 markdown 代码块标记。

                【输出格式】
                [{"assignTo":"员工id","title":"步骤标题","instruction":"任务说明","dependsOn":[]}]
                """.formatted(objective, cards, participants.size(), Math.max(2, participants.size() + 1));
    }

    /** 解析模型输出：剥离可能的代码块标记，校验结构与引用合法性 */
    private List<PlanStep> parse(String raw, List<CapabilityCard> participants) {
        if (raw == null || raw.isBlank()) return null;
        String json = raw.trim()
                .replaceAll("^```(json)?", "")
                .replaceAll("```$", "")
                .trim();
        // 截取第一个 [ 到最后一个 ]，容忍模型前后多余文字
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start < 0 || end <= start) return null;
        try {
            JsonNode array = objectMapper.readTree(json.substring(start, end + 1));
            if (!array.isArray() || array.isEmpty()) return null;
            List<String> validIds = participants.stream().map(CapabilityCard::digitalHumanId).toList();
            List<PlanStep> steps = new ArrayList<>();
            Set<String> assignedIds = new HashSet<>();
            for (JsonNode node : array) {
                String assignTo = node.path("assignTo").asText("");
                if (!validIds.contains(assignTo)) return null; // 派给不存在的员工 → 整体非法
                assignedIds.add(assignTo);
                String title = node.path("title").asText("").trim();
                String instruction = node.path("instruction").asText("").trim();
                if (title.isEmpty() || instruction.isEmpty()) return null;
                List<Integer> deps = new ArrayList<>();
                for (JsonNode dep : node.path("dependsOn")) {
                    int index = dep.asInt(-1);
                    if (index < 0 || index >= steps.size()) return null; // 依赖必须指向已定义的前序步骤
                    deps.add(index);
                }
                steps.add(new PlanStep(assignTo, title, instruction, deps));
            }
            if (participants.size() > 1 && assignedIds.size() < participants.size()) return null;
            return steps;
        } catch (Exception e) {
            log.warn("Planner output parse failed: {}", e.getMessage());
            return null;
        }
    }
}
