package io.github.luismtueme.framework.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** The login page, at "/login". */
public class LoginPage extends BasePage {

    private static final By USERNAME = byLabel("Username");
    private static final By PASSWORD = byLabel("Password");
    private static final By SUBMIT = button("Log in");
    private static final By ERROR = By.id("login-error");
    private static final By WELCOME = By.id("welcome");

    public LoginPage(WebDriver driver, String baseUrl, Duration timeout) {
        super(driver, baseUrl, timeout);
    }

    @Override
    protected String path() {
        return "/login";
    }

    @Override
    protected By readyIndicator() {
        return SUBMIT;
    }

    public void login(String username, String password) {
        type(USERNAME, username);
        type(PASSWORD, password);
        click(SUBMIT);
    }

    /** Waits for the welcome message and returns it. */
    public String welcomeText() {
        return visible(WELCOME).getText();
    }

    /** The current error message; empty while there is none. */
    public String errorText() {
        return textOf(ERROR);
    }
}
