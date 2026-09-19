package cn.xiaofuge.deepseek.harness.infrastructure.digitalhuman;

import cn.xiaofuge.deepseek.harness.infrastructure.dao.ICollaborationDao;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.IDigitalHumanDao;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.IDigitalHumanEndpointDao;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.CollaborationPO.ArtifactPO;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.CollaborationPO.ParticipantPO;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.CollaborationPO.RoomEventPO;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.CollaborationPO.RoomPO;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.CollaborationPO.TaskPO;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.DigitalHumanEndpointPO;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.DigitalHumanPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 协作房间领域服务：房间/参与者/任务、统一事件日志、
 * Orchestrator 编排（计划/DAG/并行/交接）与远端任务转发。
 *
 * <p>事实源是 room_event 表：UI 的一切展示都来自事件投影。</p>
 */
@Service
public class CollaborationService {

    private static final Logger log = LoggerFactory.getLogger(CollaborationService.class);
    private static final int EVENT_PAGE_SIZE = 500;
    private static final Pattern LOCAL_FILE_PATTERN = Pattern.compile(
            "(?:/[^\\s<>\\\"'，。；）)]+|[A-Za-z]:\\\\[^\\s<>\\\"']+)\\.(?:docx|xlsx|xls|csv|md|markdown|html?|drawio)",
            Pattern.CASE_INSENSITIVE);

    private final ICollaborationDao collaborationDao;
    private final IDigitalHumanDao humanDao;
    private final IDigitalHumanEndpointDao endpointDao;
    private final PlannerService plannerService;
    private final RemoteAgentGateway gateway;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;

    /** roomId -> 活跃 SSE 订阅者 */
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> roomEmitters = new ConcurrentHashMap<>();

    /** 任务执行沙箱边界（taskId -> cwd + 可写根）：桌面端发消息时下发，数字人执行时透传给 Agent */
    private record SandboxScope(String cwd, List<String> sandboxRoots) {
        static final SandboxScope EMPTY = new SandboxScope(null, List.of());
    }
    private final Map<String, SandboxScope> taskSandboxScopes = new ConcurrentHashMap<>();

    /** 执行中任务注册表（taskId -> 执行 Future）：cancelTask 据此真正打断执行线程，而非只改 DB 状态 */
    private final Map<String, java.util.concurrent.FutureTask<Void>> activeRuns = new ConcurrentHashMap<>();

    /** 成员定向提问预算（taskId -> 已用次数，上限 2）：预算制替代硬禁提问 */
    private final Map<String, Integer> clarifyBudget = new ConcurrentHashMap<>();
    /** 失败自动恢复预算（taskId -> 已用次数，上限 1）：改派或重试一次，耗尽才级联终止 */
    private final Map<String, Integer> recoveryBudget = new ConcurrentHashMap<>();
    /** ASK 协议：回复末尾 [[ASK:@成员名] 问题] 表示阻塞性提问，服务端路由给目标成员作答 */
    private static final Pattern ASK_PATTERN = Pattern.compile("\\[\\[ASK:@([^\\]]+)\\]\\s*([^\\]]*)]");

    public CollaborationService(ICollaborationDao collaborationDao,
                                IDigitalHumanDao humanDao,
                                IDigitalHumanEndpointDao endpointDao,
                                PlannerService plannerService) {
        this.collaborationDao = collaborationDao;
        this.humanDao = humanDao;
        this.endpointDao = endpointDao;
        this.plannerService = plannerService;
        this.gateway = new RemoteAgentGateway();
        this.objectMapper = new ObjectMapper();
        this.executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "collab-worker");
            thread.setDaemon(true);
            return thread;
        });
    }

    // ══════════════ 房间 ══════════════

    public Map<String, Object> createRoom(String workspaceId, String title, String objective, String orchestrationMode) {
        String id = "room_" + shortId();
        collaborationDao.insertRoom(new RoomPO(id,
                workspaceId == null || workspaceId.isBlank() ? "default" : workspaceId,
                requireText(title, "房间标题不能为空"), objective,
                orchestrationMode == null || orchestrationMode.isBlank() ? "MANUAL" : orchestrationMode,
                "idle", Instant.now()));
        return roomDetail(id);
    }

    public Map<String, Object> roomDetail(String roomId) {
        RoomPO room = ensureRoom(roomId);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", room.id());
        view.put("workspaceId", room.workspaceId());
        view.put("title", room.title());
        view.put("objective", room.objective());
        view.put("orchestrationMode", room.orchestrationMode());
        view.put("runState", room.runState());
        view.put("createdAt", room.createdAt() == null ? null : room.createdAt().toString());

        List<Map<String, Object>> participants = new ArrayList<>();
        for (ParticipantPO participant : collaborationDao.listParticipants(roomId)) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("participantId", participant.id());
            p.put("digitalHumanId", participant.digitalHumanId());
            p.put("roomRole", participant.roomRole());
            p.put("presence", participant.presence());
            p.put("activeTaskLabel", participant.activeTaskLabel());
            p.put("joinedAt", participant.joinedAt() == null ? null : participant.joinedAt().toString());
            humanDao.findById(participant.digitalHumanId()).ifPresent(human -> {
                p.put("displayName", human.displayName());
                p.put("avatarRef", human.avatarRef());
                p.put("themeColor", human.themeColor());
            });
            participants.add(p);
        }
        view.put("participants", participants);

        List<Map<String, Object>> tasks = new ArrayList<>();
        for (TaskPO task : collaborationDao.listTasks(roomId)) {
            tasks.add(taskView(task));
        }
        view.put("tasks", tasks);

        List<Map<String, Object>> artifacts = new ArrayList<>();
        for (ArtifactPO artifact : collaborationDao.listArtifacts(roomId)) {
            artifacts.add(artifactView(artifact));
        }
        view.put("artifacts", artifacts);
        return view;
    }

    // ══════════════ 参与者 ══════════════

    public Map<String, Object> joinRoom(String roomId, String digitalHumanId, String roomRole) {
        ensureRoom(roomId);
        DigitalHumanPO human = humanDao.findById(digitalHumanId)
                .orElseThrow(() -> new IllegalArgumentException("数字人不存在：" + digitalHumanId));
        if (collaborationDao.findParticipant(roomId, digitalHumanId).isEmpty()) {
            collaborationDao.insertParticipant(new ParticipantPO(
                    "pt_" + shortId(), roomId, digitalHumanId,
                    roomRole == null || roomRole.isBlank() ? "member" : roomRole,
                    "idle", null, Instant.now()));
            appendEvent(roomId, null, null, digitalHumanId, "PARTICIPANT_JOINED",
                    Map.of("displayName", human.displayName()));
        }
        return roomDetail(roomId);
    }

    public Map<String, Object> leaveRoom(String roomId, String digitalHumanId) {
        ensureRoom(roomId);
        collaborationDao.removeParticipant(roomId, digitalHumanId);
        humanDao.findById(digitalHumanId).ifPresent(human ->
                appendEvent(roomId, null, null, digitalHumanId, "PARTICIPANT_LEFT",
                        Map.of("displayName", human.displayName())));
        return roomDetail(roomId);
    }

    // ══════════════ 消息入口：手动 @ / 自动分工 ══════════════

    public Map<String, Object> postMessage(String roomId, String content, List<String> mentions,
                                           String token, Map<String, String> digitalHumanTokens,
                                           String channelCode, String approvalMode,
                                           String cwd, List<String> sandboxRoots) {
        RoomPO room = ensureRoom(roomId);
        String objective = requireText(content, "消息内容不能为空");
        if (room.objective() == null || room.objective().isBlank()) {
            collaborationDao.fillObjective(roomId, objective.length() > 60 ? objective.substring(0, 60) : objective);
        }
        List<ParticipantPO> participants = collaborationDao.listParticipants(roomId);
        appendEvent(roomId, null, null, null, "MESSAGE_CREATED",
                Map.of("role", "user", "content", objective));
        collaborationDao.updateRoomState(roomId, "running");

        SandboxScope scope = new SandboxScope(cwd, sandboxRoots == null ? List.of() : sandboxRoots);
        boolean explicit = mentions != null && !mentions.isEmpty();
        if (explicit || participants.size() <= 1) {
            // 手动 @ 或单成员：直接派单，走 DAG 调度
            List<ParticipantPO> targets = resolveTargets(participants, mentions);
            List<Map<String, Object>> dispatched = new ArrayList<>();
            for (ParticipantPO target : targets) {
                TaskPO task = createTask(roomId, objective, target.digitalHumanId(), "user", null);
                taskSandboxScopes.put(task.id(), scope);
                dispatched.add(taskView(task));
            }
            schedule(roomId, token, digitalHumanTokens, channelCode, approvalMode);
            return Map.of("accepted", true, "mode", "MANUAL", "tasks", dispatched);
        }
        // 多成员未指定：AUTO_PLAN 生成分工计划
        return autoPlan(roomId, objective, participants, token, digitalHumanTokens, channelCode, approvalMode, scope);
    }

    private List<ParticipantPO> resolveTargets(List<ParticipantPO> participants, List<String> mentions) {
        if (mentions == null || mentions.isEmpty()) {
            if (participants.isEmpty()) throw new IllegalArgumentException("房间还没有数字人，请先加入");
            return List.of(participants.get(0));
        }
        List<ParticipantPO> targets = new ArrayList<>();
        for (String humanId : mentions) {
            participants.stream().filter(p -> p.digitalHumanId().equals(humanId))
                    .findFirst().ifPresent(targets::add);
        }
        if (targets.isEmpty()) throw new IllegalArgumentException("@ 的数字人不在当前房间");
        return targets;
    }

    // ══════════════ Orchestrator ══════════════

    /**
     * 自动分工：优先 LLM 规划器（按能力卡生成 DAG），失败回退规则型串行链。
     * 两条路径最终都落成同一套 Task DAG，由 schedule() 统一调度。
     */
    private Map<String, Object> autoPlan(String roomId, String objective, List<ParticipantPO> participants,
                                         String token, Map<String, String> digitalHumanTokens,
                                         String channelCode, String approvalMode, SandboxScope scope) {
        // 参与者能力卡
        List<PlannerService.CapabilityCard> cards = new ArrayList<>();
        for (ParticipantPO participant : participants) {
            DigitalHumanPO human = humanDao.findById(participant.digitalHumanId()).orElse(null);
            cards.add(new PlannerService.CapabilityCard(
                    participant.digitalHumanId(),
                    human == null ? participant.digitalHumanId() : human.displayName(),
                    human == null ? "执行任务" : human.purpose(),
                    human == null || human.roleTags() == null || human.roleTags().isBlank()
                            ? List.of() : List.of(human.roleTags().split(","))));
        }

        // LLM 规划（失败回退规则型）
        List<PlannerService.PlanStep> llmSteps = plannerService.plan(objective, cards);
        List<Map<String, Object>> steps = new ArrayList<>();
        String plannerKind;

        if (llmSteps != null && !llmSteps.isEmpty()) {
            plannerKind = "llm";
            // 按 LLM 计划建 DAG：dependsOn 是步骤序号，落成 taskId 引用
            List<String> taskIds = new ArrayList<>();
            for (int i = 0; i < llmSteps.size(); i++) {
                PlannerService.PlanStep step = llmSteps.get(i);
                String dependsOn = step.dependsOn().isEmpty() ? null
                        : step.dependsOn().stream().map(taskIds::get).reduce((a, b) -> a + "," + b).orElse(null);
                TaskPO task = createTask(roomId, step.title(), step.instruction(), step.assignTo(),
                        "orchestrator", dependsOn);
                taskSandboxScopes.put(task.id(), scope);
                taskIds.add(task.id());
                steps.add(stepView(i + 1, task.id(), step.title(), step.assignTo(), cards, step.dependsOn()));
            }
        } else {
            plannerKind = "rule";
            // 规则型兜底：让每个参与者都并行领取职责范围内的任务。
            // 这保证规划器不可用时，多数字人房间不会退化成“只有第一个人在工作”。
            int seq = 1;
            for (PlannerService.CapabilityCard card : cards) {
                String title = "步骤 " + seq + "：" + abbreviate(card.purpose(), 24);
                String instruction = "作为「" + card.displayName() + "」（职责：" + card.purpose()
                        + "），参与完成房间目标：「" + objective + "」。"
                        + "请与其他数字人并行完成你职责范围内的部分，并明确交付物。";
                TaskPO task = createTask(roomId, title, instruction, card.digitalHumanId(),
                        "orchestrator", null);
                taskSandboxScopes.put(task.id(), scope);
                steps.add(stepView(seq, task.id(), card.purpose(), card.digitalHumanId(), cards, List.of()));
                seq++;
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("objective", objective);
        payload.put("planner", plannerKind);
        payload.put("steps", steps);
        appendEvent(roomId, null, null, null, "PLAN_CREATED", payload);
        schedule(roomId, token, digitalHumanTokens, channelCode, approvalMode);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", true);
        result.put("mode", "AUTO_PLAN");
        result.put("planner", plannerKind);
        result.put("plan", steps);
        return result;
    }

    private Map<String, Object> stepView(int seq, String taskId, String title, String assignTo,
                                         List<PlannerService.CapabilityCard> cards, List<Integer> dependsOn) {
        String assigneeName = cards.stream()
                .filter(c -> c.digitalHumanId().equals(assignTo))
                .map(PlannerService.CapabilityCard::displayName)
                .findFirst().orElse(assignTo);
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("seq", seq);
        step.put("taskId", taskId);
        step.put("title", abbreviate(title, 30));
        step.put("assignTo", assignTo);
        step.put("assigneeName", assigneeName);
        step.put("dependsOn", dependsOn);
        return step;
    }

    /**
     * DAG 调度核心：找出依赖已满足且执行者有空闲并发的 READY 任务，派下去。
     * 每有任务状态变化就再跑一遍，直到没有可调度的任务。
     */
    private synchronized void schedule(String roomId, String token, Map<String, String> digitalHumanTokens,
                                      String channelCode, String approvalMode) {
        // 先级联终止上游已失败的 READY/ASSIGNED 任务：depsSatisfied 只认 COMPLETED，
        // 不级联的话这些任务永远挂起，房间无法收敛、前端"生成中"永不停止
        reapOrphanTasks(roomId);
        cascadeBlockedTasks(roomId);
        List<TaskPO> tasks = collaborationDao.listTasks(roomId);
        Map<String, String> stateById = new LinkedHashMap<>();
        Map<String, Integer> runningByHuman = new LinkedHashMap<>();
        for (TaskPO task : tasks) {
            stateById.put(task.id(), task.state());
            if ("RUNNING".equals(task.state()) && task.assignedTo() != null) {
                runningByHuman.merge(task.assignedTo(), 1, Integer::sum);
            }
        }
        boolean dispatchedAny = false;
        for (TaskPO task : tasks) {
            if (!"READY".equals(task.state())) continue;
            if (!depsSatisfied(task.dependsOn(), stateById)) continue;
            int limit = humanDao.findById(task.assignedTo())
                    .map(DigitalHumanPO::concurrencyLimit).orElse(1);
            int running = runningByHuman.getOrDefault(task.assignedTo(), 0);
            if (running >= limit) continue;
            // 占位派发
            collaborationDao.updateTaskState(task.id(), "ASSIGNED", null, null, Instant.now());
            runningByHuman.merge(task.assignedTo(), 1, Integer::sum);
            dispatchedAny = true;
            final String taskId = task.id();
            final String humanId = task.assignedTo();
            final String instruction = buildInstructionWithDeps(task, stateById);
            executor.submit(() -> runTask(roomId, taskId, humanId, instruction, token, digitalHumanTokens, channelCode, approvalMode));
        }
        // 全部任务终态 → 房间回到 done，并释放任务级内存预算
        boolean allTerminal = tasks.stream().allMatch(t ->
                List.of("COMPLETED", "FAILED", "CANCELED", "TIMEOUT").contains(t.state()));
        if (!tasks.isEmpty() && allTerminal && !dispatchedAny) {
            collaborationDao.updateRoomState(roomId, "done");
            for (TaskPO task : tasks) {
                clarifyBudget.remove(task.id());
                recoveryBudget.remove(task.id());
            }
        }
    }

    private boolean depsSatisfied(String dependsOn, Map<String, String> stateById) {
        if (dependsOn == null || dependsOn.isBlank()) return true;
        for (String dep : dependsOn.split(",")) {
            if (!"COMPLETED".equals(stateById.get(dep.trim()))) return false;
        }
        return true;
    }

    /** 服务重启或执行线程异常退出后，DB 里可能残留没有活跃 Future 的 RUNNING/ASSIGNED 任务。 */
    private void reapOrphanTasks(String roomId) {
        for (TaskPO task : collaborationDao.listTasks(roomId)) {
            if (!List.of("RUNNING", "ASSIGNED").contains(task.state())) continue;
            if (activeRuns.containsKey(task.id())) continue;
            Instant updatedAt = task.updatedAt();
            if (updatedAt == null || Duration.between(updatedAt, Instant.now()).toMinutes() < 1) continue;
            collaborationDao.updateTaskState(task.id(), "FAILED", null,
                    "执行中断：未找到活跃运行（可能服务重启）", Instant.now());
            collaborationDao.updatePresence(roomId, task.assignedTo(), "error", null);
            appendEvent(roomId, task.id(), null, task.assignedTo(), "ERROR",
                    Map.of("code", "ORPHAN_TASK", "message", "执行中断：未找到活跃运行", "recoverable", true));
            appendEvent(roomId, task.id(), null, task.assignedTo(), "TASK_STATE_CHANGED",
                    Map.of("state", "FAILED"));
        }
    }

    /** 把上游任务的交付物拼进当前任务指令（交接的语义载体） */
    private String buildInstructionWithDeps(TaskPO task, Map<String, String> stateById) {
        if (task.dependsOn() == null || task.dependsOn().isBlank()) return task.instruction();
        StringBuilder upstream = new StringBuilder();
        for (String dep : task.dependsOn().split(",")) {
            collaborationDao.findTask(dep.trim()).ifPresent(depTask ->
                    collaborationDao.listArtifacts(task.roomId()).stream()
                            .filter(a -> depTask.id().equals(a.taskId()))
                            .forEach(a -> upstream.append("\n\n--- 上游交付物：").append(a.title())
                                    .append(" ---\n").append(abbreviate(a.content(), 2000))));
        }
        return upstream.length() == 0 ? task.instruction()
                : task.instruction() + "\n" + upstream;
    }

    private TaskPO createTask(String roomId, String titleOrObjective, String assignedTo,
                              String requestedBy, String dependsOn) {
        return createTask(roomId,
                titleOrObjective.length() > 40 ? titleOrObjective.substring(0, 40) + "…" : titleOrObjective,
                titleOrObjective, assignedTo, requestedBy, dependsOn);
    }

    private TaskPO createTask(String roomId, String title, String instruction, String assignedTo,
                              String requestedBy, String dependsOn) {
        TaskPO task = new TaskPO("task_" + shortId(), roomId, null, title, instruction,
                assignedTo, requestedBy, "READY", dependsOn, null, null, null,
                "tr_" + shortId(), null, Instant.now(), Instant.now());
        collaborationDao.insertTask(task);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("assignedTo", assignedTo);
        if (dependsOn != null) payload.put("dependsOn", List.of(dependsOn.split(",")));
        appendEvent(roomId, task.id(), null, assignedTo, "TASK_CREATED", payload);
        return task;
    }

    public Map<String, Object> cancelTask(String roomId, String taskId) {
        TaskPO task = collaborationDao.findTask(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在：" + taskId));
        if (!roomId.equals(task.roomId())) {
            throw new IllegalArgumentException("任务不属于当前房间");
        }
        if (List.of("COMPLETED", "FAILED", "CANCELED", "TIMEOUT").contains(task.state())) {
            return taskView(task); // 幂等：已终态直接返回
        }
        // 1) 真正打断执行：中断执行线程 + 网关取消标志（流读循环逐行检查，命中即抛取消）
        java.util.concurrent.FutureTask<Void> future = activeRuns.get(taskId);
        if (future != null) future.cancel(true);
        gateway.cancel(taskId);
        // 2) 置终态
        collaborationDao.updateTaskState(taskId, "CANCELED", task.remoteSessionId(), null, Instant.now());
        collaborationDao.updatePresence(roomId, task.assignedTo(), "idle", null);
        clarifyBudget.remove(taskId);
        recoveryBudget.remove(taskId);
        appendEvent(roomId, taskId, null, task.assignedTo(), "TASK_STATE_CHANGED",
                Map.of("state", "CANCELED"));
        // 3) 级联终止下游（否则依赖它的 READY 任务永远挂起，前端"生成中"永不停止）
        cascadeBlockedTasks(roomId);
        settleRoomIfTerminal(roomId);
        return taskView(collaborationDao.findTask(taskId).orElse(task));
    }

    /** 上游终态为失败/取消/超时的 READY/ASSIGNED 任务级联置 FAILED，循环至不动点。 */
    private void cascadeBlockedTasks(String roomId) {
        boolean changed = true;
        while (changed) {
            changed = false;
            List<TaskPO> tasks = collaborationDao.listTasks(roomId);
            Map<String, String> stateById = new LinkedHashMap<>();
            for (TaskPO t : tasks) stateById.put(t.id(), t.state());
            for (TaskPO t : tasks) {
                if (!"READY".equals(t.state()) && !"ASSIGNED".equals(t.state())) continue;
                if (t.dependsOn() == null || t.dependsOn().isBlank()) continue;
                for (String dep : t.dependsOn().split(",")) {
                    String depState = stateById.get(dep.trim());
                    if (depState != null && List.of("FAILED", "CANCELED", "TIMEOUT").contains(depState)) {
                        collaborationDao.updateTaskState(t.id(), "FAILED", null,
                                "上游任务" + ("CANCELED".equals(depState) ? "已取消" : "失败") + "，级联终止", Instant.now());
                        collaborationDao.updatePresence(roomId, t.assignedTo(), "idle", null);
                        appendEvent(roomId, t.id(), null, t.assignedTo(), "ERROR",
                                Map.of("code", "UPSTREAM_FAILED", "message", "上游任务未成功，本任务自动终止",
                                        "recoverable", true));
                        appendEvent(roomId, t.id(), null, t.assignedTo(), "TASK_STATE_CHANGED",
                                Map.of("state", "FAILED"));
                        stateById.put(t.id(), "FAILED");
                        changed = true;
                        break;
                    }
                }
            }
        }
    }

    /** 全部任务终态时把房间收敛为 done。 */
    private void settleRoomIfTerminal(String roomId) {
        List<TaskPO> tasks = collaborationDao.listTasks(roomId);
        boolean allTerminal = !tasks.isEmpty() && tasks.stream().allMatch(t ->
                List.of("COMPLETED", "FAILED", "CANCELED", "TIMEOUT").contains(t.state()));
        if (allTerminal) collaborationDao.updateRoomState(roomId, "done");
    }

    /**
     * 审批通过后恢复任务：把 WAITING_APPROVAL 的任务置回 READY 并重新调度。
     * 拒绝走 cancelTask。
     */
    public Map<String, Object> resumeTask(String roomId, String taskId,
                                          String token, Map<String, String> digitalHumanTokens,
                                          String channelCode, String approvalMode) {
        TaskPO task = collaborationDao.findTask(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在：" + taskId));
        if (!"WAITING_APPROVAL".equals(task.state())) {
            throw new IllegalArgumentException("任务不在等待审批状态（当前：" + task.state() + "）");
        }
        collaborationDao.updateTaskState(taskId, "READY", null, null, Instant.now());
        appendEvent(roomId, taskId, null, task.assignedTo(), "APPROVAL_RESOLVED",
                Map.of("taskId", taskId, "verdict", "ALLOW"));
        schedule(roomId, token, digitalHumanTokens, channelCode, approvalMode);
        return taskView(collaborationDao.findTask(taskId).orElse(task));
    }

    /** 失败/取消/完成后的任务重新入队，保留原执行者与依赖。 */
    public Map<String, Object> retryTask(String roomId, String taskId,
                                         String token, Map<String, String> digitalHumanTokens,
                                         String channelCode, String approvalMode) {
        TaskPO task = collaborationDao.findTask(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在：" + taskId));
        if (!roomId.equals(task.roomId())) {
            throw new IllegalArgumentException("任务不属于当前房间");
        }
        collaborationDao.updateTaskState(taskId, "READY", null, null, Instant.now());
        collaborationDao.updatePresence(roomId, task.assignedTo(), "idle", null);
        recoveryBudget.remove(taskId); // 手动重试视为全新尝试，重置自动恢复预算
        appendEvent(roomId, taskId, null, task.assignedTo(), "TASK_RETRIED",
                Map.of("taskId", taskId, "assignedTo", task.assignedTo()));
        appendEvent(roomId, taskId, null, task.assignedTo(), "TASK_STATE_CHANGED",
                Map.of("state", "READY"));
        collaborationDao.updateRoomState(roomId, "running");
        schedule(roomId, token, digitalHumanTokens, channelCode, approvalMode);
        return taskView(collaborationDao.findTask(taskId).orElse(task));
    }

    /** 转派任务给房间内其他数字人，并重新入队。 */
    public Map<String, Object> reassignTask(String roomId, String taskId, String targetDigitalHumanId,
                                            String token, Map<String, String> digitalHumanTokens,
                                            String channelCode, String approvalMode) {
        TaskPO task = collaborationDao.findTask(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在：" + taskId));
        if (!roomId.equals(task.roomId())) {
            throw new IllegalArgumentException("任务不属于当前房间");
        }
        if (collaborationDao.findParticipant(roomId, targetDigitalHumanId).isEmpty()) {
            throw new IllegalArgumentException("目标数字人不在当前房间");
        }
        collaborationDao.updateTaskAssignment(taskId, targetDigitalHumanId, "READY", Instant.now());
        if (task.assignedTo() != null && !task.assignedTo().equals(targetDigitalHumanId)) {
            collaborationDao.updatePresence(roomId, task.assignedTo(), "idle", null);
        }
        collaborationDao.updatePresence(roomId, targetDigitalHumanId, "idle", null);
        appendEvent(roomId, taskId, null, targetDigitalHumanId, "TASK_REASSIGNED",
                Map.of("taskId", taskId, "from", task.assignedTo(), "to", targetDigitalHumanId));
        appendEvent(roomId, taskId, null, targetDigitalHumanId, "TASK_STATE_CHANGED",
                Map.of("state", "READY", "assignedTo", targetDigitalHumanId));
        collaborationDao.updateRoomState(roomId, "running");
        schedule(roomId, token, digitalHumanTokens, channelCode, approvalMode);
        return taskView(collaborationDao.findTask(taskId).orElse(task));
    }

    // ══════════════ 任务执行 ══════════════

    private void runTask(String roomId, String taskId, String digitalHumanId,
                         String instruction, String token, Map<String, String> digitalHumanTokens,
                         String channelCode, String approvalMode) {
        DigitalHumanPO human = humanDao.findById(digitalHumanId).orElse(null);
        DigitalHumanEndpointPO endpoint = endpointDao.findByHumanId(digitalHumanId).orElse(null);
        String displayName = human == null ? digitalHumanId : human.displayName();

        collaborationDao.updateTaskState(taskId, "RUNNING", null, null, Instant.now());
        collaborationDao.updatePresence(roomId, digitalHumanId, "working", taskTitle(taskId));
        appendEvent(roomId, taskId, null, digitalHumanId, "TASK_STATE_CHANGED", Map.of("state", "RUNNING"));

        // 重试场景下清除上一次执行的取消残留
        gateway.clearCancelled(taskId);

        StringBuilder finalText = new StringBuilder();
        GatewayEventSink sink = new GatewayEventSink(roomId, taskId, digitalHumanId, finalText);
        // 桌面端随消息下发的沙箱边界：cwd = 会话归属项目；可写根 = @ 工程 + 资源目录
        SandboxScope scope = taskSandboxScopes.getOrDefault(taskId, SandboxScope.EMPTY);
        try {
            String effectiveApprovalMode = effectiveApprovalMode(human, approvalMode);
            if (endpoint == null || "local-dsh".equals(endpoint.endpointType()) || endpoint.baseUrl() == null) {
                String roleInstruction = "[角色设定] 你是「" + displayName + "」，职责："
                        + (human == null ? "通用智能体" : human.purpose())
                        + "。请以该身份完成任务。\n"
                        + "[协作规则] 你在多数字人协作房间中工作。不要调用 ask_user_question 向用户提问。"
                        + "遇到信息不足时，默认基于已有上下文和上游交付物自主做出最合理的假设并继续推进，在结果中注明假设即可。"
                        + "仅当缺失信息是阻塞性的（拿不到就无法产出有效交付物）且房间内其他成员可能知道答案时，"
                        + "才可在回复的最后单独一行输出：[[ASK:@成员名] 你的问题]（整个任务最多 1 次，问题要具体、对方可一句话回答），"
                        + "提问后本任务会等待对方答复并自动续跑。除此之外请正常完成任务。\n\n" + instruction;
                runWithTimeout(taskId, () ->
                        gateway.runLocal(taskId, roleInstruction, channelCode, effectiveApprovalMode, sink,
                                scope.cwd(), scope.sandboxRoots()));
            } else if ("a2a".equals(endpoint.endpointType())) {
                String remoteToken = tokenForHuman(token, digitalHumanTokens, digitalHumanId);
                String remoteInstruction = remoteInstruction(human, displayName, instruction);
                runWithTimeout(taskId, () ->
                        gateway.runA2a(taskId, endpoint.baseUrl(), remoteToken, remoteInstruction, sink));
            } else {
                String remoteToken = tokenForHuman(token, digitalHumanTokens, digitalHumanId);
                String remoteInstruction = remoteInstruction(human, displayName, instruction);
                runWithTimeout(taskId, () ->
                        gateway.runRemote(taskId, endpoint.baseUrl(), remoteToken, remoteInstruction, channelCode, effectiveApprovalMode, sink));
            }
            // 审批中断：执行在写操作审批点暂停，转入等待审批而非完成
            if (sink.approvalPending) {
                collaborationDao.updateTaskState(taskId, "WAITING_APPROVAL", null, null, Instant.now());
                collaborationDao.updatePresence(roomId, digitalHumanId, "waiting_approval", taskTitle(taskId));
                appendEvent(roomId, taskId, null, digitalHumanId, "APPROVAL_REQUIRED",
                        Map.of("digitalHumanId", digitalHumanId, "displayName", displayName,
                                "taskId", taskId, "summary", sink.approvalSummary,
                                "riskLevel", "write"));
                return; // 不 schedule：等审批 resolve 后由 resumeTask 重新派发
            }
            // 执行期间被用户取消：不覆盖状态、不归档、不发消息（cancelTask 已置 CANCELED）
            String currentState = collaborationDao.findTask(taskId).map(TaskPO::state).orElse("");
            if ("CANCELED".equals(currentState)) {
                collaborationDao.updatePresence(roomId, digitalHumanId, "idle", null);
                return;
            }
            String answer = finalText.toString();
            // 成员定向提问（预算制，每任务最多 2 轮）：目标成员作答后答案注入并自动续跑
            AskRequest ask = parseAskRequest(answer);
            if (ask != null && clarifyBudget.merge(taskId, 1, Integer::sum) <= 2) {
                String targetId = resolveMemberByName(roomId, ask.targetName());
                if (targetId != null && !targetId.equals(digitalHumanId)) {
                    DigitalHumanPO target = humanDao.findById(targetId).orElse(null);
                    String targetName = target == null ? targetId : target.displayName();
                    String question = abbreviate(ask.question(), 500);
                    String reply;
                    try {
                        reply = askPeer(roomId, taskId, digitalHumanId, displayName,
                                targetId, targetName, question,
                                token, digitalHumanTokens, channelCode);
                    } catch (Exception qaError) {
                        log.warn("Collab QA turn {} -> {} failed: {}", taskId, targetId, qaError.getMessage());
                        appendEvent(roomId, taskId, null, targetId, "ERROR",
                                Map.of("code", "QA_FAILED",
                                        "message", "答复失败：" + (qaError.getMessage() == null ? "未知错误" : qaError.getMessage()),
                                        "recoverable", true));
                        reply = null;
                    }
                    String followUp = reply == null || reply.isBlank()
                            ? "\n\n[系统提示] 成员「" + targetName + "」未能答复你的问题，请基于合理假设继续完成任务。"
                            : "\n\n[成员「" + targetName + "」的答复]\n" + abbreviate(reply, 1500);
                    collaborationDao.updateTaskInstruction(taskId, instruction + followUp, Instant.now());
                    collaborationDao.updateTaskState(taskId, "READY", null, null, Instant.now());
                    collaborationDao.updatePresence(roomId, digitalHumanId, "idle", null);
                    appendEvent(roomId, taskId, null, digitalHumanId, "TASK_STATE_CHANGED",
                            Map.of("state", "READY", "reason", "已获得成员答复，自动续跑"));
                    return; // finally 会 schedule 重新派发
                }
            }
            answer = stripAskMarkers(answer);
            // 先落任务终态和消息事件：即便后续归档/IO 短暂阻塞，UI 也不能继续显示“生成中”。
            collaborationDao.updateTaskState(taskId, "COMPLETED", null, null, Instant.now());
            collaborationDao.updatePresence(roomId, digitalHumanId, "done", null);
            appendEvent(roomId, taskId, null, digitalHumanId, "MESSAGE_CREATED",
                    Map.of("role", "assistant", "digitalHumanId", digitalHumanId,
                            "displayName", displayName, "content", answer));
            appendEvent(roomId, taskId, null, digitalHumanId, "TASK_STATE_CHANGED",
                    Map.of("state", "COMPLETED"));
            // 交付物归档（协作真正留下的东西）
            if (!answer.isBlank()) {
                archiveArtifact(roomId, taskId, digitalHumanId,
                        new ArtifactDraft("markdown", taskTitle(taskId) + " · 交付", answer, null));
            }
        } catch (java.util.concurrent.CancellationException e) {
            // 用户取消：状态已由 cancelTask 置 CANCELED，这里只复位在线状态，不覆盖终态
            log.info("Collab task {} canceled", taskId);
            collaborationDao.updatePresence(roomId, digitalHumanId, "idle", null);
        } catch (Exception e) {
            log.warn("Collab task {} failed: {}", taskId, e.getMessage());
            // 执行期间被取消的：保持 CANCELED，不覆盖为 FAILED
            String currentState = collaborationDao.findTask(taskId).map(TaskPO::state).orElse("");
            if ("CANCELED".equals(currentState)) {
                collaborationDao.updatePresence(roomId, digitalHumanId, "idle", null);
            } else if (!attemptAutoRecovery(roomId, taskId, digitalHumanId, human, e,
                    token, digitalHumanTokens)) {
                collaborationDao.updateTaskState(taskId, "FAILED", null, e.getMessage(), Instant.now());
                collaborationDao.updatePresence(roomId, digitalHumanId, "error", null);
                appendEvent(roomId, taskId, null, digitalHumanId, "ERROR",
                        Map.of("code", "TASK_FAILED", "message", e.getMessage() == null ? "执行失败" : e.getMessage(),
                                "recoverable", true));
                appendEvent(roomId, taskId, null, digitalHumanId, "TASK_STATE_CHANGED",
                        Map.of("state", "FAILED"));
            }
        } finally {
            activeRuns.remove(taskId);
            // 状态变化后再调度一轮：唤醒依赖当前任务的下游（含级联终止与房间收敛）
            schedule(roomId, token, digitalHumanTokens, channelCode, approvalMode);
        }
    }


    private String effectiveApprovalMode(DigitalHumanPO human, String requestedMode) {
        // 桌面端下发的审批模式优先：远程协作的审批节奏由桌面端统一掌控，
        // 需审批时通过运行期审批接口反馈到桌面端裁决。仅在未指定时回退数字人档案策略。
        if (requestedMode != null && !requestedMode.isBlank()) return requestedMode;
        String policy = human == null || human.approvalPolicy() == null ? "WRITE_REQUIRES_APPROVAL" : human.approvalPolicy();
        if ("ALWAYS_CONFIRM".equals(policy)) return "REQUEST_APPROVAL";
        if ("AUTO_ALLOW".equals(policy)) return "FULL_OPEN";
        return "AUTO_APPROVE";
    }

    private String tokenForHuman(String fallbackToken, Map<String, String> digitalHumanTokens, String digitalHumanId) {
        if (digitalHumanTokens != null) {
            String token = digitalHumanTokens.get(digitalHumanId);
            if (token != null && !token.isBlank()) return token;
        }
        return fallbackToken;
    }

    private String remoteInstruction(DigitalHumanPO human, String displayName, String instruction) {
        return "[角色设定] 你是「" + displayName + "」，职责："
                + (human == null ? "通用智能体" : human.purpose())
                + "。请以该身份完成任务。\n"
                + "[协作规则] 你在多数字人协作房间中工作。遇到信息不足时，默认基于上下文做出假设并继续推进，"
                + "在结果中注明假设与交付物。仅当缺失信息是阻塞性的且房间内其他成员可能知道答案时，"
                + "才可在回复的最后单独一行输出：[[ASK:@成员名] 你的问题]（整个任务最多 1 次，问题要具体、对方可一句话回答），"
                + "提问后本任务会等待对方答复并自动续跑。除此之外请正常完成任务。\n\n" + instruction;
    }

    // ══════════════ 成员定向问答（ASK 协议） ══════════════

    private record AskRequest(String targetName, String question) {
    }

    /** 取最后一个 ASK 标记（多次提问只处理最后一次）。 */
    private AskRequest parseAskRequest(String text) {
        if (text == null || text.isBlank()) return null;
        Matcher matcher = ASK_PATTERN.matcher(text);
        AskRequest found = null;
        while (matcher.find()) {
            found = new AskRequest(matcher.group(1).trim(), matcher.group(2).trim());
        }
        return found != null && !found.question().isBlank() ? found : null;
    }

    private String stripAskMarkers(String text) {
        return text == null ? "" : ASK_PATTERN.matcher(text).replaceAll("").trim();
    }

    /** 按显示名（或 id）在房间参与者中解析成员。 */
    private String resolveMemberByName(String roomId, String name) {
        if (name == null || name.isBlank()) return null;
        for (var participant : collaborationDao.listParticipants(roomId)) {
            DigitalHumanPO human = humanDao.findById(participant.digitalHumanId()).orElse(null);
            if (human == null) continue;
            if (name.equalsIgnoreCase(human.displayName()) || name.equalsIgnoreCase(human.id())) {
                return human.id();
            }
        }
        return null;
    }

    /**
     * 问答回合：提问入群聊 → 目标成员轻量作答（独立取消 key、只读审批模式、不得再问）→ 答案入群聊。
     * 只负责对话本身；答案注入与续跑由调用方处理。
     */
    private String askPeer(String roomId, String taskId, String askerId, String askerName,
                           String targetId, String targetName, String question,
                           String token, Map<String, String> digitalHumanTokens, String channelCode) throws Exception {
        appendEvent(roomId, taskId, null, askerId, "MESSAGE_CREATED",
                Map.of("role", "assistant", "digitalHumanId", askerId, "displayName", askerName,
                        "content", "[向 " + targetName + " 提问] " + question));
        DigitalHumanEndpointPO endpoint = endpointDao.findByHumanId(targetId).orElse(null);
        StringBuilder reply = new StringBuilder();
        RemoteAgentGateway.EventSink qaSink = new RemoteAgentGateway.EventSink() {
            @Override public void onChunk(String text) {
                reply.append(text);
            }

            @Override public void onReasoning(String text) {
            }

            @Override public void onToolCall(String toolName, String summary) {
            }

            @Override public void onToolResult(String toolName, String summary) {
            }
        };
        String qaInstruction = "[协作协助请求] 成员「" + askerName + "」在执行任务时向你提问：\n" + question
                + "\n请直接作答：结论先行、简要（500 字内），基于你的职责与房间已有上下文。"
                + "不要反问、不要向用户提问、不要执行长任务、不要输出 [[ASK:...]] 标记。";
        String qaKey = taskId + ":qa";
        gateway.clearCancelled(qaKey);
        String remoteToken = tokenForHuman(token, digitalHumanTokens, targetId);
        if (endpoint == null || "local-dsh".equals(endpoint.endpointType()) || endpoint.baseUrl() == null) {
            gateway.runLocal(qaKey, qaInstruction, channelCode, "FULL_OPEN", qaSink, null, null);
        } else if ("a2a".equals(endpoint.endpointType())) {
            gateway.runA2a(qaKey, endpoint.baseUrl(), remoteToken, qaInstruction, qaSink);
        } else {
            gateway.runRemote(qaKey, endpoint.baseUrl(), remoteToken, qaInstruction, channelCode, "FULL_OPEN", qaSink);
        }
        String answer = reply.toString();
        appendEvent(roomId, taskId, null, targetId, "MESSAGE_CREATED",
                Map.of("role", "assistant", "digitalHumanId", targetId, "displayName", targetName,
                        "content", "[答复 " + askerName + "] " + abbreviate(answer.isBlank() ? "（无有效答复）" : answer, 1500)));
        return answer;
    }

    // ══════════════ 失败自动恢复（改派 / 重试） ══════════════

    /**
     * 失败后的一次自动恢复机会（每任务 1 次）：
     * <ol>
     *   <li>确定性端点故障（HTTP 4xx/5xx）：改派给房间内其他有空闲并发的成员（优先同 roleTags）；</li>
     *   <li>其他失败（超时/网络抖动等）：优先改派，无候选则同员重试，指令附失败原因要求调整方法。</li>
     * </ol>
     * 返回 true 表示已入队恢复（finally 的 schedule 会派发）；false 表示预算耗尽或无候选，走级联终止。
     */
    private boolean attemptAutoRecovery(String roomId, String taskId, String failedHumanId,
                                        DigitalHumanPO failedHuman, Exception cause,
                                        String token, Map<String, String> digitalHumanTokens) {
        if (recoveryBudget.merge(taskId, 1, Integer::sum) > 1) return false;
        boolean endpointSurface = cause instanceof RemoteAgentGateway.HttpSurfaceException;
        TaskPO task = collaborationDao.findTask(taskId).orElse(null);
        if (task == null) return false;
        String reason = cause.getMessage() == null ? "执行失败" : cause.getMessage();
        String recoveryNote = "\n\n[自动恢复] 上一次执行失败：" + abbreviate(reason, 300)
                + "。请调整方法或路径规避该问题后重试。";

        String targetId = pickReassignTarget(roomId, failedHumanId, failedHuman);
        if (targetId != null) {
            DigitalHumanPO target = humanDao.findById(targetId).orElse(null);
            String targetName = target == null ? targetId : target.displayName();
            collaborationDao.updateTaskAssignment(taskId, targetId, "READY", Instant.now());
            collaborationDao.updateTaskInstruction(taskId, task.instruction() + recoveryNote, Instant.now());
            collaborationDao.updatePresence(roomId, failedHumanId, "idle", null);
            appendEvent(roomId, taskId, null, targetId, "TASK_REASSIGNED",
                    Map.of("taskId", taskId, "from", failedHumanId, "to", targetId,
                            "auto", true, "reason", abbreviate(reason, 200)));
            appendEvent(roomId, taskId, null, targetId, "TASK_STATE_CHANGED",
                    Map.of("state", "READY", "reason", "自动改派给 " + targetName + " 重试"));
            log.info("Collab task {} auto-reassigned {} -> {} after failure: {}", taskId, failedHumanId, targetId, reason);
            return true;
        }
        if (!endpointSurface) {
            // 无改派候选且非确定性端点故障：同员重试一次
            collaborationDao.updateTaskState(taskId, "READY", null, null, Instant.now());
            collaborationDao.updateTaskInstruction(taskId, task.instruction() + recoveryNote, Instant.now());
            collaborationDao.updatePresence(roomId, failedHumanId, "idle", null);
            appendEvent(roomId, taskId, null, failedHumanId, "TASK_RETRIED",
                    Map.of("taskId", taskId, "assignedTo", failedHumanId,
                            "auto", true, "reason", abbreviate(reason, 200)));
            appendEvent(roomId, taskId, null, failedHumanId, "TASK_STATE_CHANGED",
                    Map.of("state", "READY", "reason", "自动重试"));
            log.info("Collab task {} auto-retried on {} after failure: {}", taskId, failedHumanId, reason);
            return true;
        }
        recoveryBudget.remove(taskId); // 不恢复则释放预算，供后续手动 retryTask 使用
        return false;
    }

    /** 在房间参与者中挑改派候选：有空闲并发者优先同 roleTags，其次任意空闲者。 */
    private String pickReassignTarget(String roomId, String excludeHumanId, DigitalHumanPO failedHuman) {
        Map<String, Long> runningByHuman = new LinkedHashMap<>();
        for (TaskPO t : collaborationDao.listTasks(roomId)) {
            if ("RUNNING".equals(t.state()) && t.assignedTo() != null) {
                runningByHuman.merge(t.assignedTo(), 1L, Long::sum);
            }
        }
        String best = null;
        int bestScore = -1;
        for (var participant : collaborationDao.listParticipants(roomId)) {
            String id = participant.digitalHumanId();
            if (id.equals(excludeHumanId)) continue;
            DigitalHumanPO human = humanDao.findById(id).orElse(null);
            if (human == null) continue;
            int limit = human.concurrencyLimit() <= 0 ? 1 : human.concurrencyLimit();
            if (runningByHuman.getOrDefault(id, 0L) >= limit) continue;
            boolean sameRole = failedHuman != null && failedHuman.roleTags() != null
                    && failedHuman.roleTags().equalsIgnoreCase(human.roleTags());
            int score = (sameRole ? 2 : 0) + ("working".equals(participant.presence()) ? 0 : 1);
            if (score > bestScore) {
                bestScore = score;
                best = id;
            }
        }
        return best;
    }

    /** 带看门狗的任务执行：超时（默认 6 分钟）未完成则抛错，杜绝 ask_user_question 之类的死等 */
    private void runWithTimeout(String taskId, ThrowingRunnable runnable) throws Exception {
        java.util.concurrent.FutureTask<Void> future = new java.util.concurrent.FutureTask<>(() -> {
            runnable.run();
            return null;
        });
        Thread thread = new Thread(future, "collab-task-" + taskId);
        thread.setDaemon(true);
        activeRuns.put(taskId, future);
        thread.start();
        try {
            future.get(6, java.util.concurrent.TimeUnit.MINUTES);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            thread.interrupt();
            // 同步网关取消标志：socket 读阻塞在线循环时 interrupt 无效，靠逐行取消检查打断
            gateway.cancel(taskId);
            throw new IllegalStateException("执行超时（可能因等待用户输入而中断），已自动停止。可重试或调整任务描述。");
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) throw exception;
            throw new IllegalStateException(String.valueOf(cause));
        } finally {
            activeRuns.remove(taskId);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private record ArtifactDraft(String kind, String title, String content, String identity) {
    }

    private String archiveArtifact(String roomId, String taskId, String digitalHumanId, ArtifactDraft draft) {
        ArtifactPO artifact = new ArtifactPO("art_" + shortId(), roomId, taskId, digitalHumanId,
                draft.kind(), draft.title(), 1, draft.content(), taskId, Instant.now());
        collaborationDao.insertArtifact(artifact);
        appendTaskArtifact(taskId, artifact.id());

        Map<String, Object> eventPayload = new LinkedHashMap<>();
        eventPayload.put("artifactId", artifact.id());
        eventPayload.put("title", artifact.title());
        eventPayload.put("kind", artifact.kind());
        readJsonObject(artifact.content()).ifPresent(node -> {
            putText(eventPayload, "filePath", node.path("filePath"));
            putText(eventPayload, "mimeType", node.path("mimeType"));
        });
        appendEvent(roomId, taskId, null, digitalHumanId, "ARTIFACT_CREATED", eventPayload);
        return artifact.id();
    }

    private void appendTaskArtifact(String taskId, String artifactId) {
        Set<String> ids = new HashSet<>();
        collaborationDao.findTask(taskId).ifPresent(task -> {
            if (task.outputArtifacts() == null || task.outputArtifacts().isBlank()) return;
            for (String id : task.outputArtifacts().split(",")) {
                if (!id.isBlank()) ids.add(id.trim());
            }
        });
        ids.add(artifactId);
        collaborationDao.updateTaskArtifacts(taskId, String.join(",", ids), Instant.now());
    }

    private List<ArtifactDraft> extractToolArtifacts(String toolName, String summary, String rawResult) {
        List<ArtifactDraft> drafts = new ArrayList<>();
        readJsonObject(rawResult).ifPresent(node -> {
            String title = textOr(node.path("title"), toolArtifactTitle(toolName, summary));
            JsonNode echartsOption = node.path("echartsOption");
            if (!echartsOption.isMissingNode() && !echartsOption.isNull()) {
                Map<String, Object> content = new LinkedHashMap<>();
                content.put("kind", "echarts");
                content.put("title", title);
                content.put("summary", textOr(node.path("summary"), summary));
                content.put("echartsOption", objectMapper.convertValue(echartsOption, Object.class));
                drafts.add(new ArtifactDraft("echarts", title, toJson(content), "echarts:" + title));
            }

            String filePath = textOr(node.path("filePath"), textOr(node.path("path"), ""));
            if (!filePath.isBlank()) {
                String kind = kindFromFilePath(filePath, toolName);
                Map<String, Object> content = new LinkedHashMap<>();
                content.put("kind", kind);
                content.put("title", title);
                content.put("filePath", filePath);
                content.put("mimeType", mimeTypeOf(filePath, kind));
                putText(content, "summary", node.path("summary"));
                putText(content, "previewMarkdown", node.path("markdown"));
                drafts.add(new ArtifactDraft(kind, title, toJson(content), kind + ":" + filePath));
            }
        });

        if (drafts.isEmpty()) {
            for (String filePath : extractLocalFilePaths(rawResult + "\n" + summary)) {
                String kind = kindFromFilePath(filePath, toolName);
                Map<String, Object> content = new LinkedHashMap<>();
                content.put("kind", kind);
                content.put("title", toolArtifactTitle(toolName, summary));
                content.put("filePath", filePath);
                content.put("mimeType", mimeTypeOf(filePath, kind));
                drafts.add(new ArtifactDraft(kind, toolArtifactTitle(toolName, summary), toJson(content), kind + ":" + filePath));
            }
        }
        return drafts;
    }

    private java.util.Optional<JsonNode> readJsonObject(String value) {
        if (value == null || value.isBlank()) return java.util.Optional.empty();
        try {
            JsonNode node = objectMapper.readTree(value.trim());
            if (node.isTextual()) node = objectMapper.readTree(node.asText());
            return node.isObject() ? java.util.Optional.of(node) : java.util.Optional.empty();
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
    }

    private List<String> extractLocalFilePaths(String text) {
        List<String> paths = new ArrayList<>();
        Matcher matcher = LOCAL_FILE_PATTERN.matcher(text == null ? "" : text);
        while (matcher.find()) {
            String path = matcher.group().replaceAll("[.,;:!?，。；：！？）)」】'\"]+$", "");
            if (!paths.contains(path)) paths.add(path);
        }
        return paths;
    }

    private String toolArtifactTitle(String toolName, String summary) {
        String base = summary == null || summary.isBlank() ? toolName : summary;
        if (base == null || base.isBlank()) return "工具产物";
        return base.length() > 48 ? base.substring(0, 48) + "…" : base;
    }

    private String kindFromFilePath(String filePath, String toolName) {
        String lower = filePath == null ? "" : filePath.toLowerCase();
        String tool = toolName == null ? "" : toolName.toLowerCase();
        if (lower.endsWith(".docx") || tool.contains("doc")) return "word";
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.endsWith(".csv") || tool.contains("sheet")) return "excel";
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) return "markdown";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "html";
        if (lower.endsWith(".drawio")) return "drawio";
        return "file";
    }

    private String mimeTypeOf(String filePath, String kind) {
        String lower = filePath == null ? "" : filePath.toLowerCase();
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".csv")) return "text/csv";
        if ("markdown".equals(kind)) return "text/markdown";
        if ("html".equals(kind)) return "text/html";
        if ("drawio".equals(kind) || lower.endsWith(".drawio")) return "application/x-drawio";
        return "application/octet-stream";
    }

    private String textOr(JsonNode node, String fallback) {
        return node != null && node.isTextual() ? node.asText() : fallback;
    }

    private void putText(Map<String, Object> target, String key, JsonNode node) {
        if (node != null && node.isTextual() && !node.asText().isBlank()) {
            target.put(key, node.asText());
        }
    }

    private class GatewayEventSink implements RemoteAgentGateway.EventSink {
        private final String roomId;
        private final String taskId;
        private final String digitalHumanId;
        private final StringBuilder finalText;
        private final Set<String> artifactKeys = ConcurrentHashMap.newKeySet();
        /** 已落库的工具调用 callId：上游对同一次调用会双发（step_break ×2），按 callId 去重防重复卡片 */
        private final Set<String> toolCallKeys = ConcurrentHashMap.newKeySet();
        /** 执行在写操作审批点中断（Agent 流内出现需审批的工具调用后流结束） */
        volatile boolean approvalPending = false;
        volatile String approvalSummary = "";

        GatewayEventSink(String roomId, String taskId, String digitalHumanId, StringBuilder finalText) {
            this.roomId = roomId;
            this.taskId = taskId;
            this.digitalHumanId = digitalHumanId;
            this.finalText = finalText;
        }

        @Override public void onChunk(String text) {
            finalText.append(text);
            appendEvent(roomId, taskId, null, digitalHumanId, "MESSAGE_CHUNK", Map.of("text", text));
        }

        @Override public void onReasoning(String text) {
            // 推理增量不进房间事件日志：UI 不渲染该类型，且高频（含大量空块）事件
            // 会迅速挤占事件窗口（此前一个房间 500 条里 216 条是推理块），
            // 把真正重要的消息/工具/交付事件挤出首页，表现为"协作只剩一个数字人"。
        }

        @Override public void onToolCall(String toolName, String summary) {
            onToolCall(toolName, summary, "");
        }

        @Override public void onToolCall(String toolName, String summary, String callId) {
            boolean duplicated = callId != null && !callId.isBlank() && !toolCallKeys.add(callId);
            if (duplicated) return; // 同一次调用的重复事件：只保留第一张卡
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("toolName", toolName);
            payload.put("summary", summary);
            if (callId != null && !callId.isBlank()) payload.put("callId", callId);
            appendEvent(roomId, taskId, null, digitalHumanId, "TOOL_CALL", payload);
        }

        @Override public void onToolResult(String toolName, String summary) {
            onToolResult(toolName, summary, "");
        }

        @Override public void onToolResult(String toolName, String summary, String rawResult) {
            onToolResult(toolName, summary, rawResult, "");
        }

        @Override public void onToolResult(String toolName, String summary, String rawResult, String callId) {
            // 审批被裁决（放行/拒绝）后工具必然产出结果：清除挂起标记，
            // 任务就地完成，避免误入 WAITING_APPROVAL 触发 resumeTask 全量重跑。
            approvalPending = false;
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("toolName", toolName);
            payload.put("summary", summary);
            if (callId != null && !callId.isBlank()) payload.put("callId", callId);
            appendEvent(roomId, taskId, null, digitalHumanId, "TOOL_RESULT", payload);
            for (ArtifactDraft draft : extractToolArtifacts(toolName, summary, rawResult)) {
                if (draft.identity() != null && !artifactKeys.add(draft.identity())) continue;
                try {
                    archiveArtifact(roomId, taskId, digitalHumanId, draft);
                } catch (Exception exception) {
                    log.debug("Skip tool artifact archive for {}: {}", toolName, exception.getMessage());
                }
            }
        }

        @Override public void onApprovalRequired(String toolName, String summary) {
            approvalPending = true;
            approvalSummary = summary == null || summary.isBlank() ? toolName : summary;
        }
    }

    // ══════════════ 事件流 ══════════════

    public SseEmitter subscribe(String roomId, long afterSeq) {
        ensureRoom(roomId);
        SseEmitter emitter = new SseEmitter(0L);
        // 先注册订阅者再补历史：补历史期间新产生的事件会走实时推送（客户端按 seq 去重），
        // 保证不丢事件；backlog 必须按页循环取满——历史事件超过单页 500 条时，
        // 只取一页会让后面的成员消息/交付物永远进不了 UI（表现为"协作只剩一个数字人"）。
        roomEmitters.computeIfAbsent(roomId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable cleanup = () -> roomEmitters.getOrDefault(roomId, new CopyOnWriteArrayList<>()).remove(emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());
        executor.submit(() -> {
            try {
                long cursor = afterSeq;
                while (true) {
                    List<RoomEventPO> batch = collaborationDao.listEventsAfter(roomId, cursor, EVENT_PAGE_SIZE);
                    for (RoomEventPO event : batch) {
                        emitter.send(SseEmitter.event().name("room-event").data(toJson(eventView(event))));
                        cursor = event.seq();
                    }
                    if (batch.size() < EVENT_PAGE_SIZE) break;
                }
            } catch (Exception e) {
                cleanup.run();
                emitter.complete();
            }
        });
        return emitter;
    }

    private void appendEvent(String roomId, String taskId, String participantId,
                             String digitalHumanId, String type, Map<String, Object> payload) {
        try {
            synchronized (("seq_" + roomId).intern()) {
                long seq = collaborationDao.maxEventSeq(roomId) + 1;
                RoomEventPO event = new RoomEventPO("evt_" + shortId(), roomId, seq,
                        taskId, participantId, digitalHumanId, type, Instant.now(), "room",
                        toJson(payload), taskId);
                collaborationDao.insertEvent(event);
                push(roomId, event);
            }
        } catch (Exception e) {
            log.warn("Append room event failed ({} {}): {}", roomId, type, e.getMessage());
        }
    }

    private void push(String roomId, RoomEventPO event) {
        List<SseEmitter> emitters = roomEmitters.get(roomId);
        if (emitters == null || emitters.isEmpty()) return;
        String json = toJson(eventView(event));
        // 遍历副本：send 失败时会从 emitters 移除失效连接，
        // 直接在原列表上 for-each 移除会触发 ConcurrentModificationException，
        // 异常虽被上层 appendEvent 吞掉，但同批其余在线订阅者会漏推这一条事件
        for (SseEmitter emitter : new ArrayList<>(emitters)) {
            try {
                emitter.send(SseEmitter.event().name("room-event").data(json));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    public List<Map<String, Object>> listEvents(String roomId, long afterSeq) {
        List<Map<String, Object>> views = new ArrayList<>();
        for (RoomEventPO event : collaborationDao.listEventsAfter(roomId, afterSeq, EVENT_PAGE_SIZE)) {
            views.add(eventView(event));
        }
        return views;
    }

    // ══════════════ 视图与工具 ══════════════

    private Map<String, Object> taskView(TaskPO task) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("taskId", task.id());
        view.put("roomId", task.roomId());
        view.put("title", task.title());
        view.put("instruction", task.instruction());
        view.put("assignedTo", task.assignedTo());
        view.put("state", task.state());
        view.put("dependsOn", task.dependsOn() == null ? List.of() : List.of(task.dependsOn().split(",")));
        view.put("traceId", task.traceId());
        view.put("errorSummary", task.errorSummary());
        view.put("createdAt", task.createdAt() == null ? null : task.createdAt().toString());
        humanDao.findById(task.assignedTo()).ifPresent(human -> {
            view.put("assigneeName", human.displayName());
            view.put("assigneeAvatar", human.avatarRef());
            view.put("assigneeColor", human.themeColor());
        });
        return view;
    }

    private Map<String, Object> artifactView(ArtifactPO artifact) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("artifactId", artifact.id());
        view.put("taskId", artifact.taskId());
        view.put("producerId", artifact.producerId());
        view.put("kind", artifact.kind());
        view.put("title", artifact.title());
        view.put("version", artifact.version());
        view.put("content", artifact.content());
        readJsonObject(artifact.content()).ifPresent(node -> {
            putText(view, "filePath", node.path("filePath"));
            putText(view, "mimeType", node.path("mimeType"));
            putText(view, "previewMarkdown", node.path("previewMarkdown"));
            putText(view, "summary", node.path("summary"));
            JsonNode echartsOption = node.path("echartsOption");
            if (!echartsOption.isMissingNode() && !echartsOption.isNull()) {
                view.put("echartsOption", objectMapper.convertValue(echartsOption, Object.class));
            }
        });
        view.put("createdAt", artifact.createdAt() == null ? null : artifact.createdAt().toString());
        humanDao.findById(artifact.producerId()).ifPresent(human -> {
            view.put("producerName", human.displayName());
            view.put("producerColor", human.themeColor());
        });
        return view;
    }

    private Map<String, Object> eventView(RoomEventPO event) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", event.id());
        view.put("roomId", event.roomId());
        view.put("seq", event.seq());
        view.put("taskId", event.taskId());
        view.put("participantId", event.participantId());
        view.put("digitalHumanId", event.digitalHumanId());
        view.put("type", event.eventType());
        view.put("occurredAt", event.occurredAt() == null ? null : event.occurredAt().toString());
        try {
            view.put("payload", objectMapper.readValue(event.payload() == null ? "{}" : event.payload(), Map.class));
        } catch (Exception e) {
            view.put("payload", Map.of());
        }
        view.put("traceId", event.traceId());
        if (event.digitalHumanId() != null) {
            humanDao.findById(event.digitalHumanId()).ifPresent(human -> {
                view.put("displayName", human.displayName());
                view.put("avatarRef", human.avatarRef());
                view.put("themeColor", human.themeColor());
            });
        }
        return view;
    }

    private String taskTitle(String taskId) {
        return collaborationDao.findTask(taskId).map(TaskPO::title).orElse(null);
    }

    private RoomPO ensureRoom(String roomId) {
        return collaborationDao.findRoom(roomId)
                .orElseThrow(() -> new IllegalArgumentException("房间不存在：" + roomId));
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private String abbreviate(String text, int max) {
        if (text == null) return "";
        String singleLine = text.replaceAll("\\s+", " ").trim();
        return singleLine.length() > max ? singleLine.substring(0, max) + "…" : singleLine;
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
