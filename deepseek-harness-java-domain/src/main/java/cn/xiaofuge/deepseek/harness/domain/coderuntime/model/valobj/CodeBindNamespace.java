package cn.xiaofuge.deepseek.harness.domain.coderuntime.model.valobj;

import java.util.List;

/**
 * Namespace of bindings for code execution.
 *
 * @param globals top-level names exposed to the program
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record CodeBindNamespace(List<CodeRuntimeBinding> globals) {
    /**
     * CodeRuntimeBinding 定义当前模块中的class契约与职责。
     */
    public static class CodeRuntimeBinding {
        public final String name;
        public final String value;
        public final boolean isFunction;
        /**
         * 处理Code Runtime Binding相关逻辑。
         */
        public CodeRuntimeBinding(String name, String value, boolean isFunction) {
            this.name = name;
            this.value = value;
            this.isFunction = isFunction;
        }
    }
}
