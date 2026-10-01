package io.github.luismtueme.acceptance.steps;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.luismtueme.acceptance.support.Eventually;
import io.github.luismtueme.acceptance.support.TestContext;
import io.github.luismtueme.framework.pages.ItemsPage;
import io.restassured.response.Response;
import java.util.Map;
import java.util.Optional;

public class ItemsSteps {

    private final TestContext context;
    private String itemAddedOnPage;

    public ItemsSteps(TestContext context) {
        this.context = context;
    }

    private ItemsPage itemsPage() {
        return context.itemsPage();
    }

    @Given("I am on the items page")
    public void iAmOnTheItemsPage() {
        itemsPage().open();
    }

    @Then("I see the items page")
    public void iSeeTheItemsPage() {
        assertThat(itemsPage().currentPath())
                .as("not redirected to the login page")
                .isEqualTo("/items");
        assertThat(itemsPage().title()).isEqualTo("Items | Example Application");
    }

    @Given("an item named {string} exists")
    public void anItemNamedExists(String name) {
        Response response = context.authenticatedApi().post("/api/items", Map.of("name", name));
        assertThat(response.statusCode()).as("create item through the API").isEqualTo(201);
        context.cleanUpItem(response.jsonPath().getLong("id"));
    }

    @When("I add an item named {string} on the items page")
    public void iAddAnItemNamedOnTheItemsPage(String name) {
        itemsPage().addItem(name);
        itemAddedOnPage = name;
        // Register the cleanup as soon as the page shows the new item's id
        Eventually.assertThat(() -> assertThat(itemsPage().itemId(name)).isPresent());
        context.cleanUpItem(itemsPage().itemId(name).orElseThrow());
    }

    @Then("the items list shows {string}")
    public void theItemsListShows(String name) {
        Eventually.assertThat(() -> assertThat(itemsPage().itemNames()).contains(name));
    }

    @Then("the API returns the item added on the page")
    public void theApiReturnsTheItemAddedOnThePage() {
        Optional<Long> id = itemsPage().itemId(itemAddedOnPage);
        assertThat(id).as("id of \"%s\" on the page", itemAddedOnPage).isPresent();
        Response response = context.authenticatedApi().get("/api/items/" + id.orElseThrow());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString("name")).isEqualTo(itemAddedOnPage);
    }
}
