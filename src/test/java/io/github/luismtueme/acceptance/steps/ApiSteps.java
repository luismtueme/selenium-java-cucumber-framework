package io.github.luismtueme.acceptance.steps;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.luismtueme.acceptance.support.Eventually;
import io.github.luismtueme.acceptance.support.TestContext;
import io.github.luismtueme.framework.api.JsonMatch;
import io.restassured.response.Response;
import java.util.Map;

/** API and database steps. No browser: tag these scenarios @api (and @db for the database checks). */
public class ApiSteps {

    private final TestContext context;

    public ApiSteps(TestContext context) {
        this.context = context;
    }

    @Given("I am authenticated with the API")
    public void iAmAuthenticatedWithTheApi() {
        context.useAuthenticatedApi();
    }

    @When("I create an item named {string}")
    public void iCreateAnItemNamed(String name) {
        Response response = context.api().post("/api/items", Map.of("name", name));
        context.setLastResponse(response);
        if (response.statusCode() == 201) {
            long id = response.jsonPath().getLong("id");
            context.setCreatedItemId(id);
            context.cleanUpItem(id);
        }
    }

    @When("I delete the created item")
    public void iDeleteTheCreatedItem() {
        context.setLastResponse(context.api().delete("/api/items/" + context.createdItemId()));
    }

    @When("I list the items without authenticating")
    public void iListTheItemsWithoutAuthenticating() {
        context.setLastResponse(context.anonymousApi().get("/api/items"));
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        Response response = context.lastResponse();
        assertThat(response.statusCode())
                .as("HTTP status, body: %s", response.asString())
                .isEqualTo(status);
    }

    /** The response body contains the given JSON: listed fields must match, other fields are ignored. */
    @Then("the response body matches:")
    public void theResponseBodyMatches(String expectedJson) {
        String body = context.lastResponse().asString();
        assertThat(JsonMatch.differences(expectedJson, body))
                .as("response body %s", body)
                .isEmpty();
    }

    @Then("the created item can be fetched by its id")
    public void theCreatedItemCanBeFetchedByItsId() {
        Response created = context.lastResponse();
        Response fetched = context.api().get("/api/items/" + context.createdItemId());
        assertThat(fetched.statusCode()).isEqualTo(200);
        assertThat(JsonMatch.differences(created.asString(), fetched.asString()))
                .isEmpty();
    }

    // --- Database (the app may save asynchronously, so these checks retry) ------------------------------------------

    @Then("the database has the created item named {string}")
    public void theDatabaseHasTheCreatedItemNamed(String name) {
        long id = context.createdItemId();
        Eventually.assertThat(() -> assertThat(context.db().one("SELECT name FROM items WHERE id = ?", id))
                .hasValueSatisfying(row -> assertThat(row).containsEntry("name", name)));
    }

    @Then("the database no longer has the created item")
    public void theDatabaseNoLongerHasTheCreatedItem() {
        long id = context.createdItemId();
        Eventually.assertThat(() -> assertThat(context.db().one("SELECT id FROM items WHERE id = ?", id))
                .isEmpty());
    }
}
