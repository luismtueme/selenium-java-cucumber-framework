package io.github.luismtueme.acceptance.steps;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.luismtueme.acceptance.support.Eventually;
import io.github.luismtueme.acceptance.support.TestContext;
import io.github.luismtueme.framework.config.Credentials;
import io.github.luismtueme.framework.pages.LoginPage;

public class LoginSteps {

    private final TestContext context;

    public LoginSteps(TestContext context) {
        this.context = context;
    }

    private LoginPage loginPage() {
        return context.loginPage();
    }

    private Credentials credentials() {
        return context.config().requireCredentials();
    }

    @Given("I am on the login page")
    public void iAmOnTheLoginPage() {
        loginPage().open();
    }

    @When("I log in with the configured credentials")
    public void iLogInWithTheConfiguredCredentials() {
        loginPage().login(credentials().username(), credentials().password());
    }

    @When("I log in as the configured user with the password {string}")
    public void iLogInAsTheConfiguredUserWithThePassword(String password) {
        loginPage().login(credentials().username(), password);
    }

    @Then("I am welcomed as the configured user")
    public void iAmWelcomedAsTheConfiguredUser() {
        assertThat(loginPage().welcomeText())
                .isEqualTo("Welcome, " + credentials().username());
    }

    @Then("I see the login error {string}")
    public void iSeeTheLoginError(String message) {
        Eventually.assertThat(() -> assertThat(loginPage().errorText()).isEqualTo(message));
    }
}
