package com.unimarket.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(
        packages = "com.unimarket",
        importOptions = {
                ImportOption.DoNotIncludeTests.class,
                WebModuleStructureTest.OnlyWebModuleClasses.class
        }
)
class WebModuleStructureTest {

    @ArchTest
    static final com.tngtech.archunit.lang.ArchRule webModuleBusinessPackagesShouldOnlyContainControllers =
            classes().that()
                    .resideInAPackage("com.unimarket.module..")
                    .should()
                    .resideInAPackage("com.unimarket.module..controller..");

    static final class OnlyWebModuleClasses implements ImportOption {
        @Override
        public boolean includes(Location location) {
            String path = location.asURI().toString().replace('\\', '/');
            return path.contains("unimarket-web/target/classes");
        }
    }
}
