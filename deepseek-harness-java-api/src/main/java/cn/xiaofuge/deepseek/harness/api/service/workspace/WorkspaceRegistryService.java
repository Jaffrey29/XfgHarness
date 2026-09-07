package cn.xiaofuge.deepseek.harness.api.service.workspace;

import cn.xiaofuge.deepseek.harness.api.dto.workspace.CreateWorkspaceRequest;
import cn.xiaofuge.deepseek.harness.api.dto.workspace.RenameWorkspaceRequest;
import cn.xiaofuge.deepseek.harness.api.dto.workspace.ReorderWorkspaceRequest;
import cn.xiaofuge.deepseek.harness.api.response.workspace.WorkspaceEntryResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 基于文件系统的工作区注册服务。
 * <p>
 * 执行流程：
 * 1. 读取配置中的初始工作区；
 * 2. 合并磁盘上的实际目录；
 * 3. 维护排序文件并对外提供增删改查能力。
 *
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class WorkspaceRegistryService {

    private static final String ORDER_FILE = ".workspace-order.json";

    private final Map<String, String> configuredWorkspaces = new LinkedHashMap<>();
    private final String workspaceRoot;

    /**
     * 初始化工作区注册中心。
     * <p>流程：读取持久化工作区目录 → 建立内存索引 → 对外提供查询和变更。</p>
     */
    public WorkspaceRegistryService(
            @Value("${harness.workspaces:}") String configuredWorkspaces,
            @Value("${harness.workspace.root:./workspaces}") String workspaceRoot
    ) {
        this.workspaceRoot = workspaceRoot;
        if (configuredWorkspaces != null && !configuredWorkspaces.isBlank()) {
            for (String ws : configuredWorkspaces.split(",\\s*")) {
                String name = ws.trim();
                if (name.isBlank()) {
                    continue;
                }
                this.configuredWorkspaces.put(safeName(name), resolve(name).toString());
            }
        }
    }

    /**
     * 读取工作区列表。
     * <p>
     * 执行流程：
     * 1. 合并配置工作区和磁盘目录；
     * 2. 按持久化顺序文件排序；
     * 3. 转成 `WorkspaceEntryResponse` 返回。
     */
    public List<WorkspaceEntryResponse> listWorkspaces() throws IOException {
        Map<String, String> merged = new LinkedHashMap<>(configuredWorkspaces);
        Path root = rootPath();
        if (Files.isDirectory(root)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
                for (Path child : stream) {
                    if (Files.isDirectory(child) && !isOrderFile(child)) {
                        merged.putIfAbsent(child.getFileName().toString(), child.toAbsolutePath().toString());
                    }
                }
            }
        }
        return toEntries(ordered(merged));
    }

    /**
     * 创建一个新的工作区目录。
     * <p>
     * 执行流程：
     * 1. 校验名称是否合法；
     * 2. 创建目录；
     * 3. 返回最新工作区列表。
     */
    public List<WorkspaceEntryResponse> createWorkspace(CreateWorkspaceRequest request) throws IOException {
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("workspace name must not be blank");
        }
        String safe = safeName(name);
        Path dir = resolve(safe);
        if (Files.exists(dir)) {
            throw new IllegalArgumentException("workspace already exists: " + safe);
        }
        Files.createDirectories(dir);
        return listWorkspaces();
    }

    /**
     * 删除一个工作区目录。
     * <p>
     * 执行流程：
     * 1. 解析并清理目录名；
     * 2. 删除磁盘目录与顺序记录；
     * 3. 返回最新工作区列表。
     */
    public List<WorkspaceEntryResponse> deleteWorkspace(String name) throws IOException {
        String safe = safeName(name);
        Path dir = resolve(safe);
        if (Files.isDirectory(dir)) {
            deleteRecursively(dir);
        }
        configuredWorkspaces.remove(safe);
        removeFromOrder(safe);
        return listWorkspaces();
    }

    /**
     * 重命名工作区目录。
     * <p>
     * 执行流程：
     * 1. 校验旧名称与新名称；
     * 2. 移动目录；
     * 3. 同步更新配置和排序文件。
     */
    public List<WorkspaceEntryResponse> renameWorkspace(String name, RenameWorkspaceRequest request) throws IOException {
        String oldSafe = safeName(name);
        String newName = request.name() == null ? "" : request.name().trim();
        if (newName.isBlank()) {
            throw new IllegalArgumentException("workspace name must not be blank");
        }
        String newSafe = safeName(newName);
        Path oldDir = resolve(oldSafe);
        if (!Files.isDirectory(oldDir)) {
            throw new IllegalArgumentException("workspace not found: " + oldSafe);
        }
        Path newDir = resolve(newSafe);
        if (Files.exists(newDir)) {
            throw new IllegalArgumentException("workspace already exists: " + newSafe);
        }
        Files.move(oldDir, newDir, StandardCopyOption.ATOMIC_MOVE);
        if (configuredWorkspaces.containsKey(oldSafe)) {
            configuredWorkspaces.remove(oldSafe);
            configuredWorkspaces.put(newSafe, newDir.toString());
            renameInOrder(oldSafe, newSafe);
        }
        return listWorkspaces();
    }

    /**
     * 重新排序工作区。
     * <p>
     * 执行流程：
     * 1. 读取前端传入的顺序；
     * 2. 清理非法值并写入排序文件；
     * 3. 返回最新列表。
     */
    public List<WorkspaceEntryResponse> reorderWorkspaces(ReorderWorkspaceRequest request) throws IOException {
        List<String> order = request.order() == null ? List.of() : new ArrayList<>(request.order());
        Set<String> seen = new LinkedHashSet<>();
        for (String n : order) {
            if (n != null && !n.isBlank()) {
                seen.add(safeName(n));
            }
        }
        saveOrder(seen);
        return listWorkspaces();
    }

    /**
     * 列出目录下可见子目录。
     * <p>
     * 执行流程：
     * 1. 解析目标路径；
     * 2. 读取直接子目录；
     * 3. 返回用于目录选择器的字符串列表。
     */
    public List<String> listDirectories(String path) throws IOException {
        Path base = path == null || path.isBlank() ? rootPath().toAbsolutePath() : Paths.get(path);
        if (!Files.isDirectory(base)) {
            return List.of();
        }
        List<String> entries = new ArrayList<>();
        Path rootAbs = rootPath().toAbsolutePath();
        Path parent = base.getParent();
        if (parent != null && !base.toAbsolutePath().normalize().equals(rootAbs)) {
            entries.add("../");
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(base)) {
            for (Path child : stream) {
                if (isOrderFile(child)) {
                    continue;
                }
                String fileName = child.getFileName().toString();
                if (Files.isDirectory(child)) {
                    entries.add(fileName + "/");
                }
            }
        }
        return entries;
    }

    /**
     * 处理根路径 路径。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @return 方法执行结果。
     */
    private Path rootPath() {
        return Paths.get(workspaceRoot).toAbsolutePath().normalize();
    }

    /**
     * 解析e。
     */
    private Path resolve(String name) {
        return rootPath().resolve(safeName(name)).normalize();
    }

    /**
     * 处理safe 名称。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @param name 名称。
     * @return 方法执行结果。
     */
    private static String safeName(String name) {
        return name.replaceAll("[^\\p{L}\\p{N}._-]", "_");
    }

    /**
     * 判断Order File是否成立。
     */
    private static boolean isOrderFile(Path child) {
        return child != null && child.getFileName() != null
                && ORDER_FILE.equals(child.getFileName().toString());
    }

    /**
     * 处理order 路径。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @return 方法执行结果。
     */
    private Path orderPath() {
        return rootPath().resolve(ORDER_FILE);
    }

    /**
     * 读取 Order。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @return 方法执行结果。
     */
    private List<String> readOrder() {
        try {
            Path p = orderPath();
            if (Files.isRegularFile(p)) {
                String raw = Files.readString(p, StandardCharsets.UTF_8);
                List<String> order = new ArrayList<>();
                for (String line : raw.split("\n")) {
                    String n = line.trim();
                    if (!n.isBlank()) {
                        order.add(n);
                    }
                }
                return order;
            }
        } catch (IOException ignored) {
            // 读取排序文件失败时忽略，回退为默认顺序。
        }
        return List.of();
    }

    /**
     * 保存 Order。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @param order 排序规则。
     */
    private void saveOrder(Set<String> order) throws IOException {
        Files.createDirectories(rootPath());
        Files.writeString(orderPath(), String.join("\n", order), StandardCharsets.UTF_8);
    }

    /**
     * 移除 From Order。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @param name 名称。
     */
    private void removeFromOrder(String name) throws IOException {
        List<String> order = new ArrayList<>(readOrder());
        order.remove(name);
        saveOrder(new LinkedHashSet<>(order));
    }

    /**
     * 处理rename In Order。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @param oldName old 名称参数。
     * @param newName new 名称参数。
     */
    private void renameInOrder(String oldName, String newName) throws IOException {
        List<String> order = new ArrayList<>(readOrder());
        List<String> updated = new ArrayList<>();
        for (String n : order) {
            updated.add(n.equals(oldName) ? newName : n);
        }
        if (!updated.contains(newName)) {
            updated.add(newName);
        }
        saveOrder(new LinkedHashSet<>(updated));
    }

    /**
     * 处理ordered。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @param Map<String 映射 String参数。
     * @param merged merged参数。
     * @return 方法执行结果。
     */
    private Map<String, String> ordered(Map<String, String> merged) {
        List<String> order = readOrder();
        Map<String, String> result = new LinkedHashMap<>();
        for (String name : order) {
            String path = merged.remove(name);
            if (path != null) {
                result.put(name, path);
            }
        }
        result.putAll(merged);
        return result;
    }

    /**
     * 删除 Recursively。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @param dir dir参数。
     */
    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                        // 单个文件删除失败时跳过，继续删除其余节点。
                    }
                });
    }

    /**
     * 转换为 Entries。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @param Map<String 映射 String参数。
     * @param nameToPath 名称 To 路径参数。
     * @return 转换后的目标对象。
     */
    private static List<WorkspaceEntryResponse> toEntries(Map<String, String> nameToPath) {
        List<WorkspaceEntryResponse> entries = new ArrayList<>();
        for (Map.Entry<String, String> entry : nameToPath.entrySet()) {
            entries.add(new WorkspaceEntryResponse(entry.getKey(), entry.getValue()));
        }
        return entries;
    }
}
