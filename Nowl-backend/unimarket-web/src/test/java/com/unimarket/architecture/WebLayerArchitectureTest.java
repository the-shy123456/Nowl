package com.unimarket.architecture;

import com.unimarket.common.result.Result;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.unimarket")
class WebLayerArchitectureTest {

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule controllersShouldNotDependOnDataAccessLayer =
            noClasses().that()
                    .resideInAPackage("com.unimarket..controller..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.unimarket..mapper..",
                            "com.unimarket..repository.."
                    );

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule dataAccessLayerShouldNotDependOnControllerLayer =
            noClasses().that()
                    .resideInAnyPackage(
                            "com.unimarket..mapper..",
                            "com.unimarket..repository.."
                    )
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.unimarket..controller..");

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule serviceLayerShouldNotDependOnControllerLayer =
            noClasses().that()
                    .resideInAPackage("com.unimarket..service..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.unimarket..controller..");

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule controllersShouldNotDependOnEntityLayer =
            noClasses().that()
                    .resideInAPackage("com.unimarket..controller..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.unimarket..entity..");

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule controllerEndpointsShouldReturnResultWrapper =
            methods().that()
                    .arePublic()
                    .and().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                    .and().areDeclaredInClassesThat().resideInAPackage("com.unimarket..controller..")
                    .should()
                    .haveRawReturnType(Result.class);
}
