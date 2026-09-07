package cn.xiaofuge.deepseek.harness.domain.sdk.service;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * SdkStandaloneCondition 定义当前模块中的class契约与职责。
 */
public class SdkStandaloneCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return "true".equals(context.getEnvironment().getProperty("harness.sdk.standalone"));
    }
}
