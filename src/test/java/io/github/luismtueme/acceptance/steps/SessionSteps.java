package io.github.luismtueme.acceptance.steps;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.luismtueme.acceptance.support.TestContext;
import io.github.luismtueme.framework.browser.BrowserClock;
import java.time.Duration;

/** Time-dependent behavior, with the page's clock under test control (see {@link BrowserClock}). */
public class SessionSteps {

    private final TestContext context;

    public SessionSteps(TestContext context) {
        this.context = context;
    }

    @Given("the browser clock is under test control")
    public void theBrowserClockIsUnderTestControl() {
        BrowserClock.install(context.driver());
    }

    @When("{int} minute(s) pass(es) without activity")
    public void minutesPassWithoutActivity(int minutes) {
        BrowserClock.fastForward(context.driver(), Duration.ofMinutes(minutes));
    }

    @When("I type in the new item name")
    public void iTypeInTheNewItemName() {
        context.itemsPage().typeNewItemName("still here");
    }

    @Then("the session is still active")
    public void theSessionIsStillActive() {
        assertThat(context.itemsPage().isSessionExpiredNoticeShown())
                .as("session expired notice")
                .isFalse();
        assertThat(context.itemsPage().isAddItemFormShown()).as("add item form").isTrue();
    }

    @Then("the session has expired")
    public void theSessionHasExpired() {
        assertThat(context.itemsPage().isSessionExpiredNoticeShown())
                .as("session expired notice")
                .isTrue();
        assertThat(context.itemsPage().isAddItemFormShown()).as("add item form").isFalse();
    }
}
