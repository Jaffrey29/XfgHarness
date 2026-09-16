package cn.xiaofuge.deepseek.harness.domain.sandbox.service;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * 当前会话沙箱额外可写根注册表。
 * <p>调用方（如桌面端）在发起对话时把项目下授权的工程目录通过 {@code sandboxRoots}
 * 传入，由解析节点写入本注册表；fs/shell 沙箱适配器在每次工具调用时读取，
 * 用于 workspace-write 模式下放行这些目录。</p>
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Component
public class SandboxExtraRootsRegistry {

    private final AtomicReference<List<Path>> roots = new AtomicReference<>(List.of());

    /**
     * 更新当前额外根。
     * @param paths 工程目录路径列表；null 视为清空。
     */
    public void update(List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            roots.set(List.of());
            return;
        }
        List<Path> normalized = new CopyOnWriteArrayList<>();
        for (String p : paths) {
            if (p == null || p.isBlank()) continue;
            try {
                normalized.add(Path.of(p).toAbsolutePath().normalize());
            } catch (Exception ignored) {
                // 跳过非法路径
            }
        }
        roots.set(List.copyOf(normalized));
    }

    /**
     * 读取当前额外根（不可变列表）。
     */
    public List<Path> current() {
        return roots.get();
    }
}
