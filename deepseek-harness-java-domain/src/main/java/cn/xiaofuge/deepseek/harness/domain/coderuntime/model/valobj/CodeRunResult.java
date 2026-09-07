package cn.xiaofuge.deepseek.harness.domain.coderuntime.model.valobj;

/**
 * 代码执行结果。
 * <p>
 * 执行流程：
 * 1. 代码运行结束后生成结果；
 * 2. 记录成功状态、输出和错误信息；
 * 3. 返回给上层展示或继续处理。
 *
 * @param success 是否执行成功
 * @param output 标准输出内容
 * @param error 失败时的错误信息
 * @param exitCode 进程退出码
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record CodeRunResult(boolean success, String output, String error, Integer exitCode) {
    /**
     * 处理ok相关逻辑。
     */
    public static CodeRunResult ok(String output) {
        return new CodeRunResult(true, output, null, 0);
    }
    /**
     * 处理fail相关逻辑。
     */
    public static CodeRunResult fail(String error, Integer exitCode) {
        return new CodeRunResult(false, null, error, exitCode);
    }
}
