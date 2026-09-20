package cn.xiaofuge.deepseek.harness.domain.tool.fs;

import cn.xiaofuge.deepseek.harness.domain.tool.fs.adapter.port.IFsPort;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * cwd 感知的文件系统端口装饰器。
 * <p>
 * 背景：fsPort 是全局单例（baseDir = 服务进程目录），而 Agent 按请求携带自己的
 * 工作目录 cwd。此前 fs_read/fs_write/fs_edit 等工具把相对路径解析到进程目录，
 * 与桌面端展示文件卡片时使用的会话工作目录不一致，导致"文件已写出但前端提示不存在"。
 * <p>
 * 本装饰器在委派给底层端口前，把相对路径解析到 Agent 当前 cwd（支持热更新：
 * Agent 实例被复用、请求换了工程目录时，由 AgentRunFactory.updateCwd 刷新）。
 * 绝对路径原样透传，沙箱策略仍在底层端口（SandboxedFsService 多根校验）生效。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class CwdResolvingFsPort implements IFsPort {

    private final IFsPort delegate;
    private final AtomicReference<String> cwd;

    /**
     * 固定 cwd 的装饰器。
     * @param delegate 底层端口。
     * @param cwd Agent 工作目录，null/空表示不解析（沿用底层端口默认行为）。
     */
    public CwdResolvingFsPort(IFsPort delegate, String cwd) {
        this(delegate, new AtomicReference<>(cwd));
    }

    /**
     * 可热更新 cwd 的装饰器。
     * @param delegate 底层端口。
     * @param cwd cwd 引用，运行期可被 updateCwd 刷新。
     */
    public CwdResolvingFsPort(IFsPort delegate, AtomicReference<String> cwd) {
        this.delegate = delegate;
        this.cwd = cwd != null ? cwd : new AtomicReference<>(null);
    }

    /** 热更新 Agent 工作目录（Agent 实例复用且请求换工程时调用）。 */
    public void updateCwd(String newCwd) {
        if (newCwd != null && !newCwd.isBlank()) {
            this.cwd.set(newCwd);
        }
    }

    /** 当前生效的 cwd。 */
    public String currentCwd() {
        return cwd.get();
    }

    /** 相对路径解析到 Agent cwd；绝对路径与无 cwd 场景原样返回。 */
    private String resolve(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        Path p = Paths.get(path);
        if (p.isAbsolute()) {
            return path;
        }
        String base = cwd.get();
        if (base == null || base.isBlank()) {
            return path;
        }
        return Paths.get(base).toAbsolutePath().normalize().resolve(p).normalize().toString();
    }

    @Override
    public String read(String path, int offset, int limit) {
        return delegate.read(resolve(path), offset, limit);
    }

    @Override
    public void write(String path, String content) {
        delegate.write(resolve(path), content);
    }

    @Override
    public List<String> search(String directory, String pattern, int maxResults) {
        return delegate.search(resolve(directory), pattern, maxResults);
    }

    @Override
    public boolean exists(String path) {
        return delegate.exists(resolve(path));
    }

    @Override
    public List<DirEntry> listDir(String directory) {
        return delegate.listDir(resolve(directory));
    }

    @Override
    public List<String> grep(String directory, String pattern, String include, boolean isRegex, int maxResults) {
        return delegate.grep(resolve(directory), pattern, include, isRegex, maxResults);
    }

    @Override
    public byte[] readBytes(String path) {
        return delegate.readBytes(resolve(path));
    }
}
