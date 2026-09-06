package cn.walioffice.deepseek.harness.domain.coderuntime.model.valobj;

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
    public static class CodeRuntimeBinding {
        public final String name;
        public final String value;
        public final boolean isFunction;
        public CodeRuntimeBinding(String name, String value, boolean isFunction) {
            this.name = name;
            this.value = value;
            this.isFunction = isFunction;
        }
    }
}
