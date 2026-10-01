# Contributing

Thanks for helping improve the framework. This page covers how to set up, what a good change looks like, and what CI checks before anything reaches `main`.

## Setup

Install JDK 25 or newer and set `JAVA_HOME`. Everything else (Maven, drivers, browsers) is downloaded on first use.

```bash
cp .env.example .env   # optional; the demo app needs no settings
./mvnw verify
```

Or run everything in Docker, MySQL and a Selenium Grid included: `docker compose run --build --rm tests`.

## Making a change

1. Branch from `main`. Direct pushes to `main` are blocked.
2. Make the change, with tests. Framework code in `src/main` gets a unit test (`*Test.java`); a new page gets a page object and an accessibility row.
3. Before pushing, run the same checks CI runs:
   ```bash
   ./mvnw verify -DskipITs   # compile (-Werror), formatting, unit tests, coverage, lint, ArchUnit
   ./mvnw verify -Pcheck     # every Cucumber step defined exactly once
   ./mvnw verify             # everything, including every scenario
   ```
   `./mvnw spotless:apply` fixes formatting.
4. Open a PR. It merges once the `Checks` and `Tests` jobs pass, and is squash-merged so `main` stays linear.

## Conventions

**Page objects and steps**
- Page objects hold the locators, user actions and reads of what the user sees. They wait, but never assert.
- Steps call page objects and make the assertions. They never use `By` or `findElement` (ArchUnit enforces both).
- No `Thread.sleep` and no implicit waits. Wait for a condition in the page object, or wrap the assertion in `Eventually.assertThat(...)`.
- Every scenario cleans up what it creates: `context.cleanUpItem(id)` or `context.addCleanup(...)`. The run fails if items are left behind.
- Credentials and other secrets come from environment variables, never from committed files.

**Gherkin**
- Tags must be in `GherkinLint.ALLOWED_TAGS`. Add new tags there so they're documented.
- Every scenario needs a `Then`. Scenario names are unique within a feature.

**Flaky tests**
- Don't retry your way past a flaky test. Tag it `@quarantine` with a ticket (`@jira:ABC-123`), fix the cause, and remove the tag.

## Versions and changelog

The project follows [semantic versioning](https://semver.org). Add your change under **Unreleased** in [CHANGELOG.md](CHANGELOG.md). When releasing, move those entries under a new version, bump `<version>` in `pom.xml`, and tag the merge commit (`git tag vX.Y.Z && git push origin vX.Y.Z`).

The Java version is written in `pom.xml` (`<java.version>`), the `Dockerfile` and the workflows (`JAVA_VERSION`). `ConsistencyTest` fails until all three match.
