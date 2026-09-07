package cn.xiaofuge.deepseek.harness.runtime;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;

class RuntimeArchitectureTest {

    @Test
    void runtimeMustNotDependOnInfrastructure() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("cn.xiaofuge.deepseek.harness");

        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("cn.xiaofuge.deepseek.harness.runtime..")
                .should().dependOnClassesThat().resideInAPackage("cn.xiaofuge.deepseek.harness.infrastructure..")
                .check(classes);
    }

    @Test
    void domainMustNotDependOnRuntimeOrInfrastructure() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("cn.xiaofuge.deepseek.harness");

        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("cn.xiaofuge.deepseek.harness.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "cn.xiaofuge.deepseek.harness.runtime..",
                        "cn.xiaofuge.deepseek.harness.infrastructure..")
                .check(classes);
    }
}
