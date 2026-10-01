package io.github.luismtueme.framework.pages;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** The items page, at "/items". Needs a session; without one the app redirects to the login page. */
public class ItemsPage extends BasePage {

    private static final By HEADING = By.xpath("//h1[normalize-space()='Items']");
    private static final By NEW_ITEM_NAME = byLabel("New item name");
    private static final By ADD_ITEM = button("Add item");
    private static final By ITEMS = By.cssSelector("#items > li");
    private static final By SESSION_EXPIRED = By.id("session-expired");

    public ItemsPage(WebDriver driver, String baseUrl, Duration timeout) {
        super(driver, baseUrl, timeout);
    }

    @Override
    protected String path() {
        return "/items";
    }

    @Override
    protected By readyIndicator() {
        return HEADING;
    }

    public void addItem(String name) {
        type(NEW_ITEM_NAME, name);
        click(ADD_ITEM);
    }

    /** Types into the new item field without submitting (user activity, for the idle timer). */
    public void typeNewItemName(String text) {
        type(NEW_ITEM_NAME, text);
    }

    public List<String> itemNames() {
        return driver.findElements(ITEMS).stream().map(WebElement::getText).toList();
    }

    /** The id the app gave the listed item with this name, if it's listed. */
    public Optional<Long> itemId(String name) {
        return driver.findElements(ITEMS).stream()
                .filter(item -> item.getText().equals(name))
                .map(item -> Long.parseLong(item.getDomAttribute("data-id")))
                .findFirst();
    }

    public boolean isAddItemFormShown() {
        return isShown(ADD_ITEM);
    }

    public boolean isSessionExpiredNoticeShown() {
        return isShown(SESSION_EXPIRED);
    }
}
