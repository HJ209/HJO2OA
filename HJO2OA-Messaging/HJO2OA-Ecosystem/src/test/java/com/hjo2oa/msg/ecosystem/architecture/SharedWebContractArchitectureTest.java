package com.hjo2oa.msg.ecosystem.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.hjo2oa.shared.web.UseSharedWebContract;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.hjo2oa.msg.ecosystem", importOptions = ImportOption.DoNotIncludeTests.class)
class SharedWebContractArchitectureTest {

    @ArchTest
    static final ArchRule admin_controllers_should_use_shared_web_contract = classes()
            .that()
            .resideInAPackage("..interfaces..")
            .and()
            .haveSimpleNameEndingWith("AdminController")
            .or()
            .haveSimpleNameEndingWith("Endpoint")
            .should()
            .beAnnotatedWith(UseSharedWebContract.class);
}
