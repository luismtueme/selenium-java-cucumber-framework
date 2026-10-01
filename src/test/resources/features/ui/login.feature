@ui @Regression
Feature: Login
  Credentials come from APP_USERNAME and APP_PASSWORD (see .env.example),
  never from feature files. The demo app uses built-in demo credentials.

  Background:
    Given I am on the login page

  # The password field is left out: it has no ARIA role (HTML-ARIA spec). Firefox reports
  # none, Chrome and Edge report "textbox", so listing it would make the check browser-specific.
  Scenario: The page has the expected structure
    Then the page shows, in order:
      | role    | name     |
      | heading | Log in   |
      | textbox | Username |
      | button  | Log in   |

  @Smoke
  Scenario: Log in with valid credentials
    When I log in with the configured credentials
    Then I am welcomed as the configured user

  Scenario: A wrong password is rejected
    When I log in as the configured user with the password "not-the-password"
    Then I see the login error "Invalid username or password"
