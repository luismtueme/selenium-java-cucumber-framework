package io.github.luismtueme.framework.pages;

import java.net.URI;
import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Shared behavior for page objects.
 *
 * <p>Page objects expose user actions ({@code login(...)}) and reads of what the user sees ({@code welcomeText()}).
 * They wait for elements before using them, but never assert: a failure points at the step or test that made the claim.
 * ArchitectureTest enforces both rules.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;
    private final String baseUrl;

    protected BasePage(WebDriver driver, String baseUrl, Duration timeout) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, timeout);
    }

    /** Path of this page relative to the base URL, starting with "/". */
    protected abstract String path();

    /** The element that shows the page has loaded. */
    protected abstract By readyIndicator();

    public void open() {
        driver.get(baseUrl + path());
        waitUntilLoaded();
    }

    public void waitUntilLoaded() {
        visible(readyIndicator());
    }

    public String title() {
        return driver.getTitle();
    }

    /** Path of the current URL, without the query string ("/login"). */
    public String currentPath() {
        return URI.create(driver.getCurrentUrl()).getPath();
    }

    protected WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    protected void type(By locator, String text) {
        WebElement field = visible(locator);
        field.clear();
        field.sendKeys(text);
    }

    /** Current text of an element, without waiting for it to change. */
    protected String textOf(By locator) {
        return driver.findElement(locator).getText();
    }

    protected boolean isShown(By locator) {
        return driver.findElements(locator).stream().anyMatch(WebElement::isDisplayed);
    }

    /** The form control labelled {@code text}, found through its {@code <label for>}, like a screen reader does. */
    protected static By byLabel(String text) {
        return By.xpath("//*[@id=//label[normalize-space()=%s]/@for]".formatted(xpathLiteral(text)));
    }

    /** The button whose visible text is {@code text}. */
    protected static By button(String text) {
        return By.xpath("//button[normalize-space()=%s]".formatted(xpathLiteral(text)));
    }

    private static String xpathLiteral(String text) {
        if (!text.contains("'")) return "'" + text + "'";
        if (!text.contains("\"")) return "\"" + text + "\"";
        return "concat('" + text.replace("'", "', \"'\", '") + "')";
    }
}
