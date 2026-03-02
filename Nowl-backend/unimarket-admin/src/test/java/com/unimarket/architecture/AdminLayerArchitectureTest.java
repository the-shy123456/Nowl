package com.unimarket.architecture;

import com.unimarket.common.result.Result;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.unimarket")
class AdminLayerArchitectureTest {

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule adminControllersShouldNotDependOnDataAccessLayer =
            noClasses().that()
                    .resideInAPackage("com.unimarket.admin..controller..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.unimarket..mapper..",
                            "com.unimarket..repository.."
                    );

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule adminControllersShouldNotDependOnEntityLayer =
            noClasses().that()
                    .resideInAPackage("com.unimarket.admin..controller..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.unimarket..entity..");

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule adminServiceLayerShouldNotDependOnControllerLayer =
            noClasses().that()
                    .resideInAPackage("com.unimarket.admin..service..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.unimarket.admin..controller..");

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule adminControllerEndpointsShouldReturnResultWrapper =
            methods().that()
                    .arePublic()
                    .and().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                    .and().areDeclaredInClassesThat().resideInAPackage("com.unimarket.admin..controller..")
                    .should()
                    .haveRawReturnType(Result.class);
}
