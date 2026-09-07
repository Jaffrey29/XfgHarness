package cn.xiaofuge.deepseek.harness.trigger;

import cn.xiaofuge.deepseek.harness.api.response.Response;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.junit.jupiter.api.Test;

class TriggerArchitectureTest {

    @Test
    void triggerMustOnlyDependOnApiAndCase() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("cn.xiaofuge.deepseek.harness");

        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("cn.xiaofuge.deepseek.harness.trigger..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "cn.xiaofuge.deepseek.harness.domain..",
                        "cn.xiaofuge.deepseek.harness.infrastructure.."
                )
                .check(classes);
    }

    @Test
    void triggerFacadeMethodsMustReturnResponseWrapper() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("cn.xiaofuge.deepseek.harness");

        ArchRuleDefinition.methods()
                .that().areDeclaredInClassesThat()
                .resideInAnyPackage(
                        "cn.xiaofuge.deepseek.harness.trigger.service",
                        "cn.xiaofuge.deepseek.harness.trigger.service.command",
                        "cn.xiaofuge.deepseek.harness.trigger.service.query"
                )
                .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Api")
                .and().arePublic()
                .should().haveRawReturnType(Response.class)
                .check(classes);
    }

    @Test
    void triggerHttpMethodsMustReturnResponseWrapperOrSse() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("cn.xiaofuge.deepseek.harness");

        ArchRuleDefinition.methods()
                .that().arePublic().and().areDeclaredInClassesThat()
                .resideInAnyPackage(
                        "cn.xiaofuge.deepseek.harness.trigger.http",
                        "cn.xiaofuge.deepseek.harness.trigger.http.command",
                        "cn.xiaofuge.deepseek.harness.trigger.http.query"
                )
                .should(new ArchCondition<>("return Response or SseEmitter") {
                    @Override
                    public void check(JavaMethod method, ConditionEvents events) {
                        String returnType = method.getRawReturnType().getName();
                        boolean allowed = returnType.equals(Response.class.getName())
                                || returnType.equals(SseEmitter.class.getName());
                        events.add(SimpleConditionEvent.satisfied(
                                allowed,
                                method.getFullName() + (allowed ? " is allowed" : " is not allowed")
                        ));
                    }
                })
                .check(classes);
    }
}
