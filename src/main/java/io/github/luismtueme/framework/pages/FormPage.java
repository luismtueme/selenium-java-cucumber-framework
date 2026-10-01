package io.github.luismtueme.framework.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** The demo app's example form, at "/". */
public class FormPage extends BasePage {

    private static final By EXAMPLE_ACTION = button("Run example action");
    private static final By RESULT = By.id("result");
    private static final By INPUT = byLabel("Example input");
    private static final By SUBMIT = button("Submit");
    private static final By MESSAGE = By.id("message");

    public FormPage(WebDriver driver, String baseUrl, Duration timeout) {
        super(driver, baseUrl, timeout);
    }

    @Override
    protected String path() {
        return "/";
    }

    @Override
    protected By readyIndicator() {
        return SUBMIT;
    }

    public void runExampleAction() {
        click(EXAMPLE_ACTION);
    }

    /** Waits for the result to appear and returns its text. */
    public String exampleActionResult() {
        return visible(RESULT).getText();
    }

    public void submit(String value) {
        type(INPUT, value);
        click(SUBMIT);
    }

    public String message() {
        return textOf(MESSAGE);
    }
}
