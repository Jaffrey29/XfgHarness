package cn.xiaofuge.deepseek.harness.domain.coderuntime.model.valobj;

import java.util.Map;

/**
 * Request to run a program against host async bindings.
 *
 * @param code        the program to execute (JS/Python)
 * @param language    target language (js, python)
 * @param bindings    host async bindings available to the program
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record CodeRunRequest(String code, String language, Map<String, CodeBinding> bindings) {
    public static final String JS = "js";
    public static final String PYTHON = "python";

    /**
     * CodeBinding 定义当前模块中的record契约与职责。
     */
    public record CodeBinding(String name, boolean isFunction, String returnType) {}
}
