package cn.xiaofuge.deepseek.harness.cases;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;

class CaseArchitectureTest {

    @Test
    void casesMustNotDependOnRuntimeOrInfrastructure() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("cn.xiaofuge.deepseek.harness");

        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("cn.xiaofuge.deepseek.harness.cases..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "cn.xiaofuge.deepseek.harness.runtime..",
                        "cn.xiaofuge.deepseek.harness.infrastructure.."
                )
                .check(classes);
    }

    @Test
    void casesMustNotDeclareBusinessObjects() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("cn.xiaofuge.deepseek.harness.cases");

        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("cn.xiaofuge.deepseek.harness.cases..")
                .and().doNotHaveFullyQualifiedName(
                        "cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle.PluginRuntimeCommand")
                .should().haveSimpleNameEndingWith("Command")
                .orShould().haveSimpleNameEndingWith("Result")
                .orShould().haveSimpleNameEndingWith("View")
                .orShould().haveSimpleNameEndingWith("Query")
                .check(classes);
    }
}
