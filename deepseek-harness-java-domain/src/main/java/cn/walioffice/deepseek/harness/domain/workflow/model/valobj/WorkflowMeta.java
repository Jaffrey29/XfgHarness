package cn.walioffice.deepseek.harness.domain.workflow.model.valobj;

import java.util.List;
import java.util.Optional;

/**
 * 工作流元信息。
 * <p>
 * 执行流程：
 * 1. 由脚本或配置声明工作流元信息；
 * 2. 领域层读取名称、说明和阶段定义；
 * 3. 作为工作流注册与展示的基础数据。
 *
 * @param name 工作流名称
 * @param description 工作流说明
 * @param whenToUse 使用场景说明
 * @param phases 阶段定义
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record WorkflowMeta(String name, String description, Optional<String> whenToUse, List<WorkflowPhase> phases) {
    public WorkflowMeta {
        phases = phases == null ? List.of() : List.copyOf(phases);
    }
}
