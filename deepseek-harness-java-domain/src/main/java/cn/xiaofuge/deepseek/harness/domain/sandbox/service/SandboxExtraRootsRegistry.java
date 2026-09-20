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
 * <p>合并语义：多会话并发时各自 update 自己的 roots，若整体覆盖会把其他在途
 * 会话刚授权的根清掉，导致其 fs_write 被沙箱拒绝（并发污染）。因此本注册表
 * 按"合并 + 容量上限（FIFO 淘汰最旧的 50 个）"处理，桌面端为本地单用户场景，
 * 根集合轻微变宽可接受。</p>
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Component
public class SandboxExtraRootsRegistry {

    /** 容量上限：超过后淘汰最旧的根，防止长期运行无限膨胀 */
    private static final int MAX_ROOTS = 50;

    private final AtomicReference<List<Path>> roots = new AtomicReference<>(List.of());

    /**
     * 合并写入额外可写根（去重、FIFO 上限淘汰）。
     * @param paths 工程目录路径列表；null 视为本次不追加。
     */
    public void update(List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return;
        }
        List<Path> current = roots.get();
        List<Path> merged = new java.util.ArrayList<>(current);
        for (String p : paths) {
            if (p == null || p.isBlank()) continue;
            try {
                Path normalized = Path.of(p).toAbsolutePath().normalize();
                if (!merged.contains(normalized)) {
                    merged.add(normalized);
                }
            } catch (Exception ignored) {
                // 跳过非法路径
            }
        }
        // FIFO 淘汰最旧的根
        if (merged.size() > MAX_ROOTS) {
            merged = new java.util.ArrayList<>(merged.subList(merged.size() - MAX_ROOTS, merged.size()));
        }
        roots.set(List.copyOf(merged));
    }

    /** 清空全部额外根（服务停机/全部会话释放时调用）。 */
    public void clear() {
        roots.set(List.of());
    }

    /**
     * 读取当前额外根（不可变列表）。
     */
    public List<Path> current() {
        return roots.get();
    }
}
