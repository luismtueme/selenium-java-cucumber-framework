# Changelog

All notable changes to this project. The format follows [Keep a Changelog](https://keepachangelog.com), and the project uses [semantic versioning](https://semver.org).

## [Unreleased]

### Added
- Run guide (`docs/GUIDE.md`): setup on every OS, running and debugging, a worked example that adds a page end to end, pointing the framework at your own app, troubleshooting and a command cheat sheet.

### Fixed
- Docker Compose writes the Cucumber report to `target/docker-reports/` on the host. It still pointed at the removed Allure results folder, so the report stayed inside the container.
- Nightly Edge: Chromium headless now uses `--headless=new` plus `--no-sandbox` and `--disable-dev-shm-usage`. Old `--headless` made Edge on GitHub-hosted Linux exit before a session started; Chrome and Firefox were unaffected.

## [1.0.0] - 2026-10-01

Java and Selenium version of [playwright-cucumber-typescript-framework](https://github.com/luismtueme/playwright-cucumber-typescript-framework), with the same demo app and scenarios.

### Added
- Java 25, Selenium 4.50, Cucumber-JVM 8.0, JUnit 6.1 (Platform Suite), built with the Maven Wrapper.
- Page Object Model: `BasePage` with explicit waits and label/button locators, page objects for the demo app's pages, and `PageStructure` for computed ARIA role and name checks.
- `DriverFactory` for Chrome, Firefox and Edge through Selenium Manager, or a Selenium Grid (`SELENIUM_REMOTE_URL`), with WebDriver BiDi on every session.
- `BrowserClock`: controls the page's timers through a BiDi preload script, for the session idle-timeout scenarios.
- `ApiClient` on REST Assured, with every exchange attached to the Cucumber report and secrets masked.
- `DbClient` (JDBC) and `@db` scenarios against MySQL; the demo app stores items in MySQL when `DB_HOST` is set.
- axe-core accessibility checks against WCAG 2.1 A/AA, with readable violation reports.
- Per-scenario state through PicoContainer (`TestContext`), cleanups that run pass or fail, and a run-level check that no test data is left behind.
- `@authenticated` scenarios that start logged in, with a session created once per run.
- Parallel scenarios, retries in CI, and a non-blocking `@quarantine` run with its own report.
- Quality gates: compiler warnings as errors, Spotless (palantir-java-format), Gherkin lint, ArchUnit design rules, JaCoCo coverage threshold on framework code, version consistency checks, Cucumber dry run.
- GitHub Actions: Checks, Tests (with a MySQL service), report publishing to GitHub Pages, nightly Chrome/Firefox/Edge. Dependabot for Maven, Actions and Docker.
- Docker Compose environment: demo app, MySQL, Selenium Grid with Chrome, and the test runner.
