package io.github.luismtueme;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;

/**
 * The framework's design rules, checked on the compiled classes. A violation fails the build with the offending class
 * and line.
 */
class ArchitectureTest {

    private static JavaClasses classes;
    private static JavaClasses mainClasses;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
                .importPackages("io.github.luismtueme");
        mainClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("io.github.luismtueme");
    }

    private static void check(ArchRule rule) {
        rule.check(classes);
    }

    @Test
    void pageObjectsDoNotAssert() {
        check(noClasses()
                .that()
                .resideInAPackage("..framework.pages..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.assertj..", "org.junit..", "org.awaitility..")
                .because("assertions belong in steps, so a failure points at the step that made the claim"));
    }

    @Test
    void stepsUsePageObjectsInsteadOfLocators() {
        check(noClasses()
                .that()
                .resideInAPackage("..acceptance.steps..")
                .should()
                .callMethod(WebDriver.class, "findElement", By.class)
                .orShould()
                .callMethod(WebDriver.class, "findElements", By.class)
                .orShould()
                .callMethod(SearchContext.class, "findElement", By.class)
                .orShould()
                .callMethod(SearchContext.class, "findElements", By.class)
                .orShould()
                .dependOnClassesThat()
                .areAssignableTo(By.class)
                .because("locators live in page objects (src/main/.../pages), so a UI change is fixed in one place"));
    }

    @Test
    void nothingSleeps() {
        check(
                noClasses()
                        .should()
                        .callMethod(Thread.class, "sleep", long.class)
                        .orShould()
                        .callMethod(Thread.class, "sleep", Duration.class)
                        .orShould()
                        .callMethod(Thread.class, "sleep", long.class, int.class)
                        .because(
                                "fixed waits make tests slow and still flaky; wait for a condition (page objects, Eventually)"));
    }

    @Test
    void noImplicitWaits() {
        check(noClasses()
                .that()
                .doNotHaveFullyQualifiedName("io.github.luismtueme.framework.browser.DriverFactory")
                .should()
                .callMethod(WebDriver.Timeouts.class, "implicitlyWait", Duration.class)
                .because("implicit waits slow down every failing lookup and mix badly with explicit waits"));
    }

    @Test
    void frameworkCodeDoesNotDependOnTheTestLayer() {
        noClasses()
                .that()
                .resideInAnyPackage("..framework..", "..demoapp..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("io.cucumber..", "..acceptance..", "org.assertj..")
                .because("src/main is reusable by any runner; Cucumber glue lives in src/test")
                .check(mainClasses);
    }

    @Test
    void theFrameworkDoesNotDependOnTheDemoApp() {
        noClasses()
                .that()
                .resideInAPackage("..framework..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..demoapp..")
                .because("the demo app is deleted when you adopt the framework")
                .check(mainClasses);
    }
}
