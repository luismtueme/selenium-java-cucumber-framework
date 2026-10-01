# Run guide

A hands-on guide for anyone setting up, running or extending this framework: how to install it, run it, write your first scenario and point it at your own application. For the feature overview, see the [README](../README.md).

- [1. Set up](#1-set-up)
- [2. Run the tests](#2-run-the-tests)
- [3. Create tests: a worked example](#3-create-tests-a-worked-example)
- [4. Point it at your own application](#4-point-it-at-your-own-application)
- [5. Troubleshooting](#5-troubleshooting)
- [6. Command cheat sheet](#6-command-cheat-sheet)

## 1. Set up

### Prerequisites

| You need | Why | Check with |
|---|---|---|
| JDK 25 or newer | The project compiles for Java 25 | `java -version` |
| Git | To clone the repository | `git --version` |
| Chrome, Firefox or Edge | The browser under test. If it's missing, Selenium Manager downloads one | |
| Docker (optional) | `@db` scenarios against MySQL, or running everything in containers | `docker info` |

You don't need to install Maven or any browser driver. The Maven Wrapper (`./mvnw`) downloads Maven on first use, and Selenium Manager finds or downloads the right driver for your browser.

**Installing a JDK.** Any distribution works. [Eclipse Temurin](https://adoptium.net) is a good default:

- Windows: `winget install EclipseAdoptium.Temurin.25.JDK`, or download the `.msi` from adoptium.net.
- macOS: `brew install --cask temurin@25`
- Linux: your package manager, or [SDKMAN](https://sdkman.io): `sdk install java 25-tem`

If you have several JDKs, point `JAVA_HOME` at JDK 25 before running Maven. The build checks the version and stops with `This project needs JDK 25 or newer. Set JAVA_HOME to it.` otherwise.

### Install and first run

```bash
git clone https://github.com/luismtueme/selenium-java-cucumber-framework.git
cd selenium-java-cucumber-framework
./mvnw verify
```

On Windows, use `mvnw.cmd verify` in Command Prompt or PowerShell (`./mvnw` works in Git Bash).

The first run downloads Maven, the dependencies and a browser driver, so it takes a few minutes. Later runs take about a minute. `verify` does everything in order:

1. Compiles with every warning treated as an error.
2. Runs the framework's unit tests, the Gherkin lint and the architecture rules, and checks unit test coverage.
3. Starts the demo app on a free port and runs every Cucumber scenario in Chrome, 4 at a time.
4. Checks formatting.

A successful run ends with `BUILD SUCCESS`. Two scenarios are reported as skipped: the `@db` ones, which need a database (see [Database scenarios](#database-scenarios)).

Open `target/cucumber-report.html` in a browser to see every scenario and step.

### Run everything in Docker instead

If you'd rather not install a JDK, Docker Compose runs the demo app, MySQL, a Selenium Grid with Chrome, and the tests:

```bash
docker compose run --build --rm tests
docker compose down -v      # afterwards: stop everything and remove the database
```

While it runs, you can watch the browser at http://localhost:7900 (password `secret`). The report is written to `target/docker-reports/cucumber-report.html` on your machine.

### IDE setup

**IntelliJ IDEA** (Community is enough):

1. Open the folder. IntelliJ imports it as a Maven project.
2. Set the project SDK to JDK 25: File > Project Structure > Project.
3. Install the **Cucumber for Java** plugin. Feature files then get syntax highlighting, and you can Ctrl+click a step to jump to its definition.
4. To run all scenarios, run `RunCucumberIT` (`src/test/java/io/github/luismtueme/acceptance/`) like any JUnit test.

**VS Code**: install the **Extension Pack for Java** and the official **Cucumber** extension (`CucumberOpen.cucumber-official`). Run tests from the terminal with the Maven commands below.

Either way, run `./mvnw spotless:apply` before committing so formatting matches CI.

## 2. Run the tests

### Everyday commands

| Goal | Command |
|---|---|
| Everything (what CI runs) | `./mvnw verify` |
| Only the scenarios | `./mvnw verify -DskipUnitTests=true` |
| Only unit tests and lint, no browser | `./mvnw verify -DskipITs` |
| Scenarios with one tag | `./mvnw verify -DskipUnitTests=true -Dcucumber.filter.tags="@Smoke"` |
| A tag expression | `-Dcucumber.filter.tags="@ui and not @authenticated"` |
| One feature file | `./mvnw verify -DskipUnitTests=true -Dcucumber.features=classpath:features/ui/login.feature` |
| One scenario, by line | `-Dcucumber.features=classpath:features/ui/login.feature:19` |
| Check every step has a definition (fast, no browser) | `./mvnw verify -Pcheck` |

Allowed tags: `@Smoke`, `@Regression`, `@ui`, `@api`, `@db`, `@authenticated`, `@a11y`, `@quarantine` and ticket tags like `@jira:QA-123`. The Gherkin lint rejects anything else.

### Watching and debugging

- **See the browser.** Local runs show it by default. Set `HEADLESS=true` to hide it, which CI always does.
- **Slow down to watch one scenario.** Run one feature with fewer workers: `-Dworkers=1`.
- **Debug in the IDE.** Set a breakpoint in a step definition, then debug `RunCucumberIT`. With `-Dworkers=1`, scenarios run one at a time, so the breakpoint is easier to follow.
- **After a failure,** open `target/cucumber-report.html`. The failed step shows the assertion message and stack trace, and the scenario has the screenshot, URL and page source attached.

### Other browsers

```bash
TEST_BROWSER=firefox ./mvnw verify
TEST_BROWSER=edge ./mvnw verify
```

In PowerShell, set the variable first: `$env:TEST_BROWSER = "firefox"; .\mvnw.cmd verify`. Or pass it as a system property, which works the same in every shell: `./mvnw verify -DTEST_BROWSER=firefox`.

To run on a Selenium Grid instead of a local browser, start one and set `SELENIUM_REMOTE_URL`:

```bash
docker run -d -p 4444:4444 --shm-size=2g selenium/standalone-chrome:4.49.0-20260909
SELENIUM_REMOTE_URL=http://localhost:4444 ./mvnw verify -DskipUnitTests=true
```

If the Grid is in Docker and the demo app runs on your machine, the browser can't reach `127.0.0.1` on your machine. In that case use Docker Compose (above), which runs the demo app in a container the Grid can reach.

### Database scenarios

`@db` scenarios check what the API wrote, directly in MySQL. They're skipped unless `DB_HOST` is set. To run them locally:

```bash
docker run -d --name test-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=testdb -e MYSQL_USER=tester -e MYSQL_PASSWORD=tester mysql:8.4
DB_HOST=127.0.0.1 DB_USER=tester DB_PASSWORD=tester DB_NAME=testdb ./mvnw verify
```

With `DB_HOST` set, the demo app stores items in MySQL too, so the scenarios and the database see the same data.

### Settings

Put settings in a `.env` file (copy `.env.example`) so you don't repeat them on every command. Order of precedence, first match wins: `-D` system properties, environment variables, `.env`, then the defaults in `src/main/resources/config/defaults.properties`. A wrong value stops the run with the variable's name, for example `TEST_BROWSER must be one of chrome, firefox, edge, got "safari"`.

Parallelism and retries are Maven properties: `-Dworkers=2`, `-Dretries=1`. When `CI` is set, the `ci` profile uses 2 workers and 1 retry.

### Reports

| File | What it has |
|---|---|
| `target/cucumber-report.html` | Every scenario and step, filterable by status and tag. Attachments: screenshots, page source, API requests and responses (secrets masked), accessibility violations |
| `target/cucumber-quarantine-report.html` | The same, for the quarantine run |
| `target/failsafe-reports/` | JUnit XML per scenario, for CI tools |
| `target/site/jacoco/index.html` | Unit test coverage of the framework code |

### What CI does

Every pull request and push to `main` runs `.github/workflows/ci.yml`:

| Job | What it runs | Blocks merging |
|---|---|---|
| Checks | `./mvnw verify -DskipITs` (compile, unit tests, coverage, lint, architecture rules, formatting), then `./mvnw verify -Pcheck` | Yes |
| Tests | Every scenario in headless Chrome, with a MySQL service so `@db` scenarios run, then the quarantined scenarios (non-blocking) | Yes |
| Publish Cucumber Report | On `main` only: puts the report on GitHub Pages | No |

A nightly workflow runs every scenario in Chrome, Firefox and Edge at 06:00 UTC. To reproduce CI locally, run the same commands with `CI=true`.

## 3. Create tests: a worked example

This walkthrough adds a **Contact** page and tests it end to end: a page object, a feature file, step definitions, an accessibility check, then an API scenario. Every snippet below was run against the demo app.

With your own application you skip step 1, because the page already exists.

### Step 1: the page (demo app only)

Add `src/main/resources/demo-app/public/contact.html`:

```html
<!doctype html>
<html lang="en">
    <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>Contact | Example Application</title>
    </head>
    <body>
        <main>
            <h1>Contact us</h1>
            <form id="contact-form" novalidate>
                <label for="contact-name">Your name</label>
                <input id="contact-name" name="name" type="text" />
                <label for="contact-message">Message</label>
                <textarea id="contact-message" name="message"></textarea>
                <button type="submit">Send</button>
            </form>
            <p id="contact-result" role="status"></p>
        </main>
        <script>
            document.getElementById('contact-form').addEventListener('submit', (event) => {
                event.preventDefault();
                const name = document.getElementById('contact-name').value.trim();
                const message = document.getElementById('contact-message').value.trim();
                document.getElementById('contact-result').textContent =
                    name && message ? `Thanks, ${name}. We'll be in touch.` : 'Please fill in both fields';
            });
        </script>
    </body>
</html>
```

Then register its path in `PAGES` in `src/main/java/io/github/luismtueme/demoapp/DemoApp.java`:

```java
private static final Map<String, String> PAGES = Map.of(
        "/", "index.html", "/login", "login.html", "/items", "items.html", "/contact", "contact.html");
```

### Step 2: the page object

Create `src/main/java/io/github/luismtueme/framework/pages/ContactPage.java`:

```java
package io.github.luismtueme.framework.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** The contact page, at "/contact". */
public class ContactPage extends BasePage {

    private static final By NAME = byLabel("Your name");
    private static final By MESSAGE = byLabel("Message");
    private static final By SEND = button("Send");
    private static final By RESULT = By.id("contact-result");

    public ContactPage(WebDriver driver, String baseUrl, Duration timeout) {
        super(driver, baseUrl, timeout);
    }

    @Override
    protected String path() {
        return "/contact";
    }

    @Override
    protected By readyIndicator() {
        return SEND;
    }

    public void send(String name, String message) {
        type(NAME, name);
        type(MESSAGE, message);
        click(SEND);
    }

    /** The current result message; empty until the form is sent. */
    public String result() {
        return textOf(RESULT);
    }
}
```

The rules for page objects:

- **Locators are private constants.** Prefer what a user sees: `byLabel("Your name")` finds the field through its `<label>`, and `button("Send")` finds a button by its text. Use `By.id` for elements with no label, like a status message.
- **Methods are user actions** (`send`) **or reads of what the user sees** (`result`). `BasePage` waits before typing or clicking, so you don't add waits yourself.
- **No assertions.** Page objects return values, and steps decide whether those values are right. `ArchitectureTest` fails the build if a page object imports AssertJ or JUnit.
- `path()` is where `open()` navigates, and `readyIndicator()` is the element `open()` waits for before returning.

### Step 3: make it available to steps

Steps get page objects from `TestContext`, which is created fresh for each scenario. In `src/test/java/io/github/luismtueme/acceptance/support/TestContext.java`, add a field and an accessor next to the others:

```java
private ContactPage contactPage;

public ContactPage contactPage() {
    if (contactPage == null) contactPage = new ContactPage(driver(), baseUrl(), config.waitTimeout());
    return contactPage;
}
```

Then add it to the page names in `PageSteps.iOpenThePage`, so generic steps like `Given I open the contact page` work:

```java
Map<String, BasePage> pages = Map.of(
        "form", context.formPage(),
        "login", context.loginPage(),
        "items", context.itemsPage(),
        "contact", context.contactPage());
```

### Step 4: the feature

Create `src/test/resources/features/ui/contact.feature`:

```gherkin
@ui @Regression
Feature: Contact form
  Visitors can send a message without logging in.

  Background:
    Given I am on the contact page

  Scenario: The page has the expected structure
    Then the page shows, in order:
      | role    | name       |
      | heading | Contact us |
      | textbox | Your name  |
      | textbox | Message    |
      | button  | Send       |

  @Smoke
  Scenario: Send a message
    When I send the message "The hydrant on 5th Street is leaking" as "Dana"
    Then the contact page says "Thanks, Dana. We'll be in touch."

  Scenario: Both fields are required
    When I send the message "" as "Dana"
    Then the contact page says "Please fill in both fields"
```

- `@ui` gives each scenario a fresh browser. Use `@api` for scenarios that don't need one.
- The structure check reads each element's ARIA role and accessible name, the way a screen reader does. Leave out elements whose role differs between browsers; a password field, for example, is `textbox` in Chrome and has no role in Firefox.
- Every scenario needs a `Then`, scenario names must be unique, and tags must be in the allowlist. The Gherkin lint (part of `./mvnw verify`) checks all three.

### Step 5: the steps

Run `./mvnw verify -Pcheck` now. It fails and lists the three steps that have no definition yet, which is a quick way to see what to write. Create `src/test/java/io/github/luismtueme/acceptance/steps/ContactSteps.java`:

```java
package io.github.luismtueme.acceptance.steps;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.luismtueme.acceptance.support.Eventually;
import io.github.luismtueme.acceptance.support.TestContext;

public class ContactSteps {

    private final TestContext context;

    public ContactSteps(TestContext context) {
        this.context = context;
    }

    @Given("I am on the contact page")
    public void iAmOnTheContactPage() {
        context.contactPage().open();
    }

    @When("I send the message {string} as {string}")
    public void iSendTheMessageAs(String message, String name) {
        context.contactPage().send(name, message);
    }

    @Then("the contact page says {string}")
    public void theContactPageSays(String expected) {
        Eventually.assertThat(() -> assertThat(context.contactPage().result()).isEqualTo(expected));
    }
}
```

- **The constructor takes `TestContext`.** PicoContainer creates one per scenario and passes the same instance to every step class and the hooks, so steps in different classes share the browser and page objects.
- **`Eventually.assertThat`** retries the assertion until it passes or `WAIT_TIMEOUT` runs out. Use it whenever the page changes after an action. Never use `Thread.sleep`; the build rejects it.
- **Steps never use `By` or `findElement`.** Locators belong in page objects, so a UI change is fixed in one place.

### Step 6: run it

```bash
./mvnw verify -DskipUnitTests=true -Dcucumber.features=classpath:features/ui/contact.feature
```

You should see `Tests run: 3, Failures: 0`. Try breaking it on purpose: change the expected text in the feature and run again. The report shows the failed step, the expected and actual text, and a screenshot.

### Step 7: accessibility

Add the page to the public pages table in `src/test/resources/features/ui/accessibility.feature`:

```gherkin
    Examples: Public pages
      | page    |
      | form    |
      | login   |
      | contact |
```

That runs axe-core on the page against WCAG 2.1 A and AA. A violation fails the scenario and attaches a report listing each rule, the failing elements and a link explaining the fix. Pages that need a login go in the `@authenticated` examples table instead.

### An API scenario

API scenarios don't open a browser. The existing steps cover login, requests, status codes and partial JSON matching, so a new scenario often needs only one new step. Add it to `src/test/java/io/github/luismtueme/acceptance/steps/ApiSteps.java`:

```java
@When("I fetch the item with id {long}")
public void iFetchTheItemWithId(long id) {
    context.setLastResponse(context.api().get("/api/items/" + id));
}
```

Then the scenario, in `src/test/resources/features/api/items.feature`:

```gherkin
  Scenario: An item that does not exist is not found
    Given I am authenticated with the API
    When I fetch the item with id 999999
    Then the response status is 404
    And the response body matches:
      """json
      { "error": { "code": "NOT_FOUND" } }
      """
```

`the response body matches:` checks only the fields you list, so ids and timestamps don't get in the way. Every request and response is attached to the report, with passwords and tokens masked.

### Test data and cleanup

Every scenario must delete what it creates, whether it passes or fails. At the end of the run, the framework fails the build if the demo app still has items, and names them.

- After creating an item through the API: `context.cleanUpItem(id)`.
- For anything else: `context.addCleanup("delete the report", () -> ...)`.

Register the cleanup right after the thing is created. Cleanups run after the scenario in reverse order, and all of them run even if one fails. See `ItemsSteps.anItemNamedExists` for the pattern.

### A database check

`@db` scenarios read the database directly to confirm what the API wrote. Use `context.db()`, which binds values with `?` so they're never pasted into SQL, and wrap the check in `Eventually`, because applications often save asynchronously. This step is in `ApiSteps`:

```java
@Then("the database has the created item named {string}")
public void theDatabaseHasTheCreatedItemNamed(String name) {
    long id = context.createdItemId();
    Eventually.assertThat(() -> assertThat(context.db().one("SELECT name FROM items WHERE id = ?", id))
            .hasValueSatisfying(row -> assertThat(row).containsEntry("name", name)));
}
```

Tag the feature or scenario `@db` so it's skipped when no database is configured.

### Quarantining a flaky scenario

Don't add retries or waits to get past a flaky scenario. Instead:

1. Open a ticket, then tag the scenario `@quarantine @jira:QA-123`.
2. It stops running in the main suite. CI runs it in a separate step that can't fail the build, with its own report. Locally: `./mvnw verify -Pquarantine`.
3. Fix the cause and remove the tag.

### Before you push

```bash
./mvnw spotless:apply   # fix formatting
./mvnw verify           # everything CI checks
```

## 4. Point it at your own application

1. **Settings.** Copy `.env.example` to `.env` and set:
   ```bash
   BASE_URL=https://your-app.example.com
   APP_USERNAME=your-test-user
   APP_PASSWORD=your-test-password
   ```
   Use a dedicated test account. When `BASE_URL` is set, the demo app isn't started and its built-in login is never used. If your API is on another host, set `API_BASE_URL` too.

2. **Login.** The framework logs in through the API once per run and reuses the session:
   - `ApiClient.login()` posts `{ "username", "password" }` to `/api/login` and reads `token` from the response. Change the path and fields to match your API.
   - `Hooks.restoreSession()` puts that token in the browser as a cookie named `session`, so `@authenticated` scenarios start logged in. Change the cookie name, or replace the hook if your app keeps its session somewhere else (for example in local storage, with `executeScript`).
   - If your login is single sign-on, or isn't available through an API, log in through `LoginPage` in that hook instead. It's slower, but it works with anything.

3. **Page objects.** Replace `FormPage`, `LoginPage` and `ItemsPage` with your pages, following the [worked example](#step-2-the-page-object). Keep `BasePage` and `PageStructure`.

4. **Features and steps.** Replace the features under `src/test/resources/features/` and the step classes. Keep `PageSteps` (page names, structure, accessibility) and the generic API steps if they fit your API.

5. **Test data.** Write factory steps like `an item named {string} exists` for your own data, and register a cleanup for each. The leftover check at the end of the run only covers the demo app, so for your app rely on cleanups, or adapt `Hooks.checkLeftoverDataAndStop()` to query your API.

6. **Database (optional).** Set the `DB_*` variables to a test database and replace the `items` queries in the `@db` steps.

7. **Delete the demo app** once nothing uses it: the `io.github.luismtueme.demoapp` package, `src/main/resources/demo-app/`, `DemoAppTest`, and the `exec-maven-plugin` entry in `pom.xml`. Remove `AppUnderTest`'s demo branch so it always uses `BASE_URL`, and point `ExchangeRecorderTest` at a small stub server or delete it. The unit test coverage threshold (`coverage.minimum` in `pom.xml`) may need lowering until you add tests for your own framework code.

8. **CI secrets.** In GitHub, add `APP_USERNAME` and `APP_PASSWORD` as repository secrets and uncomment the lines for them in `.github/workflows/ci.yml`. Set `BASE_URL` there too.

## 5. Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `This project needs JDK 25 or newer. Set JAVA_HOME to it.` | Maven is using an older JDK. Point `JAVA_HOME` at JDK 25. `./mvnw -v` shows which one it uses |
| `./mvnw: Permission denied` (macOS, Linux) | Run `chmod +x mvnw` once |
| `TEST_BROWSER must be one of chrome, firefox, edge, got "..."` (or a similar message for another variable) | A setting has a typo. The message names the variable; check your environment and `.env` |
| `SessionNotCreatedException` | The browser couldn't start. Update the browser, or delete Selenium Manager's cache (`~/.cache/selenium`) and run again. Behind a proxy, Selenium Manager can't download drivers: set `HTTPS_PROXY` |
| `NoSuchElementException` or `TimeoutException` in a page object | The locator doesn't match, or the element never appeared. Check the screenshot and page source in the report, then the label or button text in the page object |
| A scenario passes alone but fails in parallel | It shares state with another scenario, usually test data. Give data unique names, keep state in `TestContext`, and check with `-Dworkers=1` |
| `Scenarios left N item(s) behind: [...]` at the end of the run | A scenario created data without a cleanup. Add `context.cleanUpItem(id)` right after creating it |
| `Undefined scenarios:` from `./mvnw verify -Pcheck` | A step in a feature file has no definition, or its text differs slightly. The output prints the step; add a definition or fix the wording |
| Build fails on `Architecture Violation` | You used `Thread.sleep`, an implicit wait, a locator in a step, or an assertion in a page object. The message names the class and line |
| Build fails on `spotless:check` | Formatting differs. Run `./mvnw spotless:apply` |
| `@db` scenarios are always skipped | `DB_HOST` isn't set in the environment the tests run in. Check `.env`, or pass it with `-DDB_HOST=...` |
| `Could not start the demo app on 127.0.0.1:4173` from `./mvnw exec:java` | Port 4173 is taken. Use another: `DEMO_APP_PORT=4180 ./mvnw exec:java`. Test runs pick a free port, so they're not affected |
| The structure check fails only in Firefox | Browsers compute some roles differently. Remove that row from the table |

## 6. Command cheat sheet

```bash
# Run
./mvnw verify                                                      # everything
./mvnw verify -DskipUnitTests=true                                 # scenarios only
./mvnw verify -DskipITs                                            # unit tests and lint only
./mvnw verify -DskipUnitTests=true -Dcucumber.filter.tags="@Smoke" # by tag
./mvnw verify -DskipUnitTests=true -Dcucumber.features=classpath:features/ui/login.feature
./mvnw verify -Pcheck                                              # every step defined once
./mvnw verify -Pquarantine                                         # quarantined scenarios
./mvnw verify -Dworkers=1                                          # one scenario at a time

# Browsers
TEST_BROWSER=firefox ./mvnw verify
HEADLESS=true ./mvnw verify
SELENIUM_REMOTE_URL=http://localhost:4444 ./mvnw verify

# Tools
./mvnw spotless:apply                    # fix formatting
./mvnw exec:java                         # demo app on http://127.0.0.1:4173
docker compose run --build --rm tests    # everything in Docker
docker compose down -v                   # clean up Docker

# Reports
target/cucumber-report.html
target/site/jacoco/index.html
```
