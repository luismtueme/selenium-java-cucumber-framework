package io.github.luismtueme.acceptance.steps;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.luismtueme.acceptance.support.Eventually;
import io.github.luismtueme.acceptance.support.TestContext;
import io.github.luismtueme.framework.pages.FormPage;

public class FormSteps {

    private final TestContext context;

    public FormSteps(TestContext context) {
        this.context = context;
    }

    private FormPage formPage() {
        return context.formPage();
    }

    @Given("I am on the form page")
    public void iAmOnTheFormPage() {
        formPage().open();
    }

    @When("I run the example action")
    public void iRunTheExampleAction() {
        formPage().runExampleAction();
    }

    @Then("I see the example action result")
    public void iSeeTheExampleActionResult() {
        assertThat(formPage().exampleActionResult()).isEqualTo("Example action completed");
    }

    @When("I submit the form with {string}")
    public void iSubmitTheFormWith(String value) {
        formPage().submit(value);
    }

    @Then("the form message is {string}")
    public void theFormMessageIs(String message) {
        Eventually.assertThat(() -> assertThat(formPage().message()).isEqualTo(message));
    }
}
