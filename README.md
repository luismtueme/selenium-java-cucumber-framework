# Selenium + Cucumber Automation Framework (Java)

UI, API and database test automation with [Selenium WebDriver](https://www.selenium.dev), [Cucumber](https://cucumber.io) and [JUnit](https://junit.org), following the Page Object Model.

Built on Java 25, Selenium 4.50, Cucumber-JVM 8 and JUnit 6. It has the same design, demo app and scenarios as [playwright-cucumber-typescript-framework](https://github.com/luismtueme/playwright-cucumber-typescript-framework), adapted to Selenium and the Java toolchain.

[![Selenium Tests](https://github.com/luismtueme/selenium-java-cucumber-framework/actions/workflows/ci.yml/badge.svg)](https://github.com/luismtueme/selenium-java-cucumber-framework/actions/workflows/ci.yml) · [Latest Cucumber report](https://luismtueme.github.io/selenium-java-cucumber-framework/)

## Which repo should I use?

This is one of four versions of the same framework. They share the design, the demo app and the scenarios.

| Repository | Tests are written as | Stack | Choose it when |
|---|---|---|---|
| **selenium-java-cucumber-framework** (this one) | Gherkin scenarios | Java, Selenium WebDriver | Your team works in Java, or needs Selenium: a Selenium Grid, or WebDriver-based tooling |
| [playwright-cucumber-typescript-framework](https://github.com/luismtueme/playwright-cucumber-typescript-framework) | Gherkin scenarios and Playwright specs | TypeScript, Playwright | Product owners, analysts or manual QA read or write scenarios, and the team works in TypeScript |
| [playwright-typescript-framework](https://github.com/luismtueme/playwright-typescript-framework) | Playwright specs | TypeScript, Playwright | Engineers write and read the tests. The most features and the simplest toolchain |
| [playwright-cucumber-automation-framework](https://github.com/luismtueme/playwright-cucumber-automation-framework) | Gherkin scenarios and Playwright specs | JavaScript, Playwright | You want Cucumber without a TypeScript toolchain |

## What's included

| Area | How it works |
|---|---|
| Page objects | `src/main/.../pages`: locators and user actions, explicit waits, no assertions. ArchUnit fails the build if a page object asserts or a step uses a locator |
| UI tests | Selenium 4 with Selenium Manager: no drivers to install. Chrome, Firefox and Edge, locally or on a Selenium Grid |
| API tests | `ApiClient` on REST Assured. Every request and response is attached to the Cucumber report with passwords and tokens masked |
| Database checks | `DbClient` (JDBC, MySQL, parameterized queries). `@db` scenarios check what the API wrote. CI runs them against a real MySQL |
| Configuration | One `Config` record for everything. Secrets come from environment variables or `.env`, never from committed files. Invalid values fail at startup with the variable name |
| Saved login | `@authenticated` scenarios start logged in: the session is created once per run through the API and put in the browser as a cookie |
| Test data | Every scenario deletes what it creates (`cleanUpItem`, `addCleanup`), pass or fail. The run fails if any items are left behind |
| Parallel runs | Scenarios run in parallel (JUnit Platform), each with its own browser. Dependency injection (PicoContainer) gives each scenario its own state |
| Waiting | Page objects wait for elements; steps retry assertions on changing state with `Eventually` (Awaitility). No `Thread.sleep` and no implicit waits, enforced by ArchUnit |
| Time control | `BrowserClock` replaces the page's timers through a WebDriver BiDi preload script, so a 15-minute idle timeout is tested instantly and exactly |
| Page structure | `PageStructure` reads each element's computed ARIA role and accessible name, so scenarios can check what a page contains the way a screen reader sees it |
| Accessibility | axe-core checks pages against WCAG 2.1 A/AA. Violations list the failing elements and link to the fix |
| Failure evidence | Screenshot, URL and page source attached to every failed UI scenario |
| Reporting | Cucumber's HTML report: every step, the error, attachments, filters by status and tag. Published to GitHub Pages from `main` |
| Quality gates | `-Werror` compile, Spotless formatting, Gherkin lint, ArchUnit design rules, framework unit tests with a coverage threshold, a Cucumber dry run (every step defined exactly once), all required to merge |
| Flaky tests | Tag `@quarantine` (with a ticket): it runs in a separate, non-blocking CI step and gets its own report |
| Cross-browser | Every PR runs on Chrome. A nightly job runs everything on Chrome, Firefox and Edge |
| Docker | `docker compose run --build --rm tests` runs everything against a Selenium Grid and MySQL, with nothing installed but Docker |
| Demo app | `src/main/.../demoapp`: a small web app and JSON API the scenarios run against, so everything passes out of the box |

## Quick start

Requires JDK 25 or newer. Maven isn't needed: the wrapper (`./mvnw`) downloads it. Browsers and drivers are found or downloaded by Selenium Manager.

```bash
./mvnw verify
```

That builds the framework, runs its unit tests and lint, starts the demo app, and runs every scenario in Chrome. The report is `target/cucumber-report.html`.

On Windows, use `mvnw.cmd verify` in Command Prompt or PowerShell.

## Testing your own application

1. Copy `.env.example` to `.env` and set at least:
   ```bash
   BASE_URL=https://your-app.example.com
   APP_USERNAME=your-test-user
   APP_PASSWORD=your-test-password
   ```
2. Replace the page objects in `src/main/java/.../framework/pages/` and the features and steps in `src/test/` with your own.
3. Update `AppUnderTest.sessionToken()` and the `@authenticated` hook with your login endpoint and session cookie.
4. Delete the `demoapp` package and `src/main/resources/demo-app/` once nothing points at them.

When `BASE_URL` is set, the demo app isn't started and the demo credentials are never used.

## Configuration

Settings are read in this order, first match wins: **`-D` system properties**, **environment variables**, **`.env`**, then **`src/main/resources/config/defaults.properties`** (non-secret defaults). Every variable is listed with a description in [`.env.example`](.env.example).

| Variable | Default | Purpose |
|---|---|---|
| `BASE_URL` | empty (demo app) | Application under test |
| `API_BASE_URL` | `BASE_URL` | API host, if different |
| `APP_USERNAME` / `APP_PASSWORD` | demo credentials, for the demo app only | Login for UI and API scenarios |
| `TEST_BROWSER` | `chrome` | `chrome`, `firefox` or `edge` |
| `HEADLESS` | `true` in CI, otherwise `false` | Show or hide the browser |
| `SELENIUM_REMOTE_URL` | unset | Run on a Selenium Grid, e.g. `http://localhost:4444` |
| `WINDOW_SIZE` | `1280x720` | Browser window size |
| `WAIT_TIMEOUT` / `PAGE_LOAD_TIMEOUT` | `10000` / `30000` ms | Explicit waits and page loads |
| `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME` | unset | Enables `@db` scenarios |
| `TEST_ENV` | `local` | Environment label |

Parallelism and retries are Maven properties: `-Dworkers=N` (default 4, 2 in CI) and `-Dretries=N` (default 0, 1 in CI). The `ci` profile switches on automatically when `CI` is set.

## Running tests

| Command | What it runs |
|---|---|
| `./mvnw verify` | Everything: unit tests and lint, then every scenario |
| `./mvnw verify -DskipUnitTests=true` | Only the scenarios |
| `./mvnw verify -DskipITs` | Only the framework's unit tests and lint (no browser) |
| `./mvnw verify -DskipUnitTests=true -Dcucumber.filter.tags="@Smoke"` | Scenarios by tag. Allowed tags are in `GherkinLint.ALLOWED_TAGS`: `@Smoke`, `@Regression`, `@ui`, `@api`, `@db`, `@authenticated`, `@a11y`, `@quarantine`, `@jira:ABC-123` |
| `./mvnw verify -DskipUnitTests=true -Dcucumber.features=classpath:features/ui/login.feature` | One feature file |
| `./mvnw verify -Pcheck` | Cucumber dry run: every step is defined exactly once. No browser |
| `./mvnw verify -Pquarantine` | Only `@quarantine` scenarios, reported in `target/cucumber-quarantine-report.html` |
| `TEST_BROWSER=firefox ./mvnw verify` | Everything in another browser |
| `./mvnw spotless:apply` | Fixes formatting |
| `./mvnw exec:java` | Starts the demo app on http://127.0.0.1:4173 |
| `docker compose run --build --rm tests` | Everything in Docker: demo app, MySQL, a Selenium Grid with Chrome |

`@db` scenarios run only when `DB_HOST` is set, and are reported as skipped otherwise. To run them locally:

```bash
docker run -d --name test-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=testdb -e MYSQL_USER=tester -e MYSQL_PASSWORD=tester mysql:8.4
DB_HOST=127.0.0.1 DB_USER=tester DB_PASSWORD=tester DB_NAME=testdb ./mvnw verify
```

When you run in Docker Compose, you can watch the browser at http://localhost:7900 (password `secret`).

## Project structure

```
├── src/main/java/io/github/luismtueme/
│   ├── framework/
│   │   ├── config/          # Config: loads and validates every setting
│   │   ├── browser/         # DriverFactory, BrowserClock (BiDi), AccessibilityCheck (axe)
│   │   ├── pages/           # Page objects: BasePage, FormPage, LoginPage, ItemsPage, PageStructure
│   │   ├── api/             # ApiClient (REST Assured), ExchangeRecorder, Masking, JsonMatch
│   │   ├── db/              # DbClient (JDBC)
│   │   └── a11y/            # AxeViolations: readable accessibility reports
│   └── demoapp/             # Example app under test (delete when you adopt the framework)
├── src/main/resources/
│   ├── config/defaults.properties
│   ├── framework/fake-clock.js
│   └── demo-app/public/     # The demo app's pages
├── src/test/java/io/github/luismtueme/
│   ├── acceptance/
│   │   ├── RunCucumberIT.java   # JUnit Platform suite that runs the features
│   │   ├── hooks/Hooks.java     # Lifecycle: app, browser, saved session, evidence, cleanup
│   │   ├── steps/               # Step definitions, one class per area
│   │   └── support/             # TestContext (per-scenario state), AppUnderTest, Eventually
│   ├── lint/                # GherkinLint and the test that runs it on every feature
│   ├── ArchitectureTest.java   # Design rules (ArchUnit)
│   ├── ConsistencyTest.java    # Versions written in several places stay in step
│   └── ...Test.java         # Unit tests for the framework code
├── src/test/resources/
│   ├── features/            # Gherkin: ui/, api/, db/
│   └── junit-platform.properties   # Cucumber settings
├── pom.xml
├── Dockerfile, docker-compose.yml
└── .env.example             # Every supported variable
```

## Writing tests

### A scenario

```gherkin
@ui @Regression
Feature: Login

  Scenario: Log in with valid credentials
    Given I am on the login page
    When I log in with the configured credentials
    Then I am welcomed as the configured user
```

Step classes get the scenario's `TestContext` through their constructor (PicoContainer creates one per scenario). Steps call page objects and make the assertions:

```java
public class LoginSteps {

    private final TestContext context;

    public LoginSteps(TestContext context) {
        this.context = context;
    }

    @When("I log in with the configured credentials")
    public void iLogInWithTheConfiguredCredentials() {
        Credentials credentials = context.config().requireCredentials();
        context.loginPage().login(credentials.username(), credentials.password());
    }

    @Then("I see the login error {string}")
    public void iSeeTheLoginError(String message) {
        // The error appears after an API call, so the assertion retries until it holds
        Eventually.assertThat(() -> assertThat(context.loginPage().errorText()).isEqualTo(message));
    }
}
```

- `@ui` scenarios get a fresh browser. `@api` scenarios don't open one: use `context.api()` or `context.authenticatedApi()` for requests and `context.db()` for queries.
- `@authenticated` scenarios start logged in, so they skip the login page.
- Anything a scenario creates must be cleaned up: `context.cleanUpItem(id)` or `context.addCleanup("description", () -> ...)`. Cleanups run after the scenario, pass or fail.

### A page object

```java
public class LoginPage extends BasePage {

    private static final By USERNAME = byLabel("Username");
    private static final By PASSWORD = byLabel("Password");
    private static final By SUBMIT = button("Log in");
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
}
```

Expose user actions and reads of what the user sees, and keep assertions out of page objects so a failure points at the step that made the claim. Prefer locators a user would recognize (`byLabel`, `button`) over CSS paths. `BasePage` waits for elements before using them; `open()` waits for the page's `readyIndicator()`.

Add a page object to `TestContext` (like `loginPage()`) and to the page names in `PageSteps` so `Given I open the <name> page` and the accessibility table can use it.

### Page structure

`Then the page shows, in order:` checks that the listed elements appear inside `<main>` in this order, with these computed ARIA roles and accessible names. Other elements in between are allowed:

```gherkin
Then the page shows, in order:
  | role    | name     |
  | heading | Log in   |
  | textbox | Username |
  | button  | Log in   |
```

It fails when a label or role changes, and ignores styling. When it fails, the message lists what the page does show. Leave out elements whose role differs between browsers: a password field has no ARIA role, so Firefox reports none while Chrome reports `textbox`.

### Time-dependent behavior

`BrowserClock.install(driver)` registers a WebDriver BiDi preload script that replaces `Date`, `setTimeout` and `setInterval` before the page's own scripts run. Time then stands still until `BrowserClock.fastForward(driver, duration)` moves it, firing due timers in order. Install it before opening the page:

```gherkin
Background:
  Given the browser clock is under test control
  And I am on the items page

Scenario: The session expires after 15 idle minutes
  When 14 minutes pass without activity
  Then the session is still active
  When 1 minute passes without activity
  Then the session has expired
```

### Accessibility checks

Add a row to `features/ui/accessibility.feature` for each new page. In any UI scenario you can also add `Then the page has no accessibility violations`. To skip content you don't control, pass CSS selectors to `AccessibilityCheck.analyze(driver, exclude, disableRules)`. To skip a rule, pass its id with a comment saying why.

### Quarantining a flaky test

1. Open a ticket, then tag the scenario `@quarantine @jira:QA-123`.
2. It no longer runs in the normal suite. CI runs it in a separate, non-blocking step with its own report.
3. Fix it and remove the tag. The Gherkin lint rejects `@quarantine` without a ticket.

## Reporting

Every run writes `target/cucumber-report.html`, a single self-contained file:

- Every feature, scenario and step, filterable by status and tag, with search.
- A failing step shows the assertion message and stack trace. A failed UI scenario also has its screenshot, URL and page source.
- API steps show each request and response as JSON, with `password`, `token`, `authorization`, `apiKey` and cookie values masked.
- Accessibility failures attach the axe report: each rule, the failing elements and the fix.

JUnit XML for every scenario is in `target/failsafe-reports/`. In CI, the report from every push to `main` is published to GitHub Pages (the quarantine report is at `quarantine.html`). For PRs, download the `test-artifacts` artifact from the run.

## CI

`.github/workflows/ci.yml` runs on every PR and push to `main`:

| Job | Runs |
|---|---|
| Checks | Compile with warnings as errors, Spotless, framework unit tests with a coverage threshold, ArchUnit rules, Gherkin lint, version consistency, Cucumber dry run |
| Tests | Every scenario in headless Chrome against the demo app, backed by a MySQL service container, with the leftover-data check. Then quarantined scenarios (non-blocking) |
| Publish Cucumber Report | On `main` only: deploys the report to GitHub Pages |
| Nightly Cross-Browser | Daily at 06:00 UTC (and on demand): every scenario on Chrome, Firefox and Edge. Not required to merge |

Dependabot opens weekly update PRs for Maven dependencies, GitHub Actions and Docker images. See [CONTRIBUTING.md](CONTRIBUTING.md) for how to make changes, and [CHANGELOG.md](CHANGELOG.md) for what changed in each version.

## Differences from the Playwright versions

| Playwright repos | Here |
|---|---|
| Auto-waiting locators and retrying `expect` | Explicit waits in `BasePage`, and `Eventually` (Awaitility) for assertions on changing state |
| `getByRole`, `getByLabel` | `byLabel()` and `button()` locators in `BasePage`; `PageStructure` for role and name checks |
| `page.clock` | `BrowserClock`, a WebDriver BiDi preload script |
| `toMatchAriaSnapshot()` | `Then the page shows, in order:` with computed ARIA roles and names |
| Allure report with trends and categories | Cucumber's HTML report. Allure's Cucumber plugin doesn't support Cucumber-JVM 8 yet |
| Traces and videos | Screenshot, URL and page source on failure |
| Visual comparison | Not included: Selenium has no built-in screenshot comparison |
| Each Cucumber worker starts its own demo app | One demo app per run with a thread-safe store; scenarios run in parallel threads in one JVM |

## License

MIT. See [LICENSE](LICENSE).
