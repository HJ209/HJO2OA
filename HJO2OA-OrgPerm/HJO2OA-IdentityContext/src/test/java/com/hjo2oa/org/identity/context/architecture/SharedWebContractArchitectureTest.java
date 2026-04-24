package com.hjo2oa.org.identity.context.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.hjo2oa.shared.web.UseSharedWebContract;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.hjo2oa.org.identity.context", importOptions = ImportOption.DoNotIncludeTests.class)
class SharedWebContractArchitectureTest {

    @ArchTest
    static final ArchRule controllers_should_use_shared_web_contract = classes()
            .that()
            .resideInAPackage("..interfaces..")
            .and()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .beAnnotatedWith(UseSharedWebContract.class);
}
