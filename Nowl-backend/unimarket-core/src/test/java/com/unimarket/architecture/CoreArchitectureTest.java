package com.unimarket.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.unimarket")
class CoreArchitectureTest {

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule moduleSlicesShouldBeFreeOfCycles =
            SlicesRuleDefinition.slices()
                    .matching("com.unimarket.module.(*)..")
                    .should()
                    .beFreeOfCycles();

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule serviceInterfacesShouldBeWellNamed =
            classes().that()
                    .areInterfaces()
                    .and().resideInAPackage("com.unimarket.module..service..")
                    .and().resideOutsideOfPackage("..service.impl..")
                    .should()
                    .haveSimpleNameEndingWith("Service");

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule serviceImplementationsShouldBeWellNamed =
            classes().that()
                    .resideInAPackage("com.unimarket.module..service.impl..")
                    .and().areTopLevelClasses()
                    .should()
                    .haveSimpleNameEndingWith("ServiceImpl");

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule mapperShouldBeWellNamed =
            classes().that()
                    .resideInAPackage("com.unimarket.module..mapper..")
                    .should()
                    .haveSimpleNameEndingWith("Mapper");

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule mapperTypesShouldBeInterfaces =
            classes().that()
                    .resideInAPackage("com.unimarket.module..mapper..")
                    .should()
                    .beInterfaces();

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule serviceLayerShouldNotDependOnControllerLayer =
            noClasses().that()
                    .resideInAPackage("com.unimarket.module..service..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.unimarket.module..controller..");

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule entitiesShouldNotDependOnServiceOrMapper =
            noClasses().that()
                    .resideInAPackage("com.unimarket.module..entity..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.unimarket.module..service..",
                            "com.unimarket.module..mapper..",
                            "com.unimarket.module..controller.."
                    );
}
