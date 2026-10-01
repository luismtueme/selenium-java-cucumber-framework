@ui
Feature: Example form
  As a user of the application
  I want feedback when I submit the example form
  So that I know whether my input was accepted

  Background:
    Given I am on the form page

  Scenario: The page has the expected structure
    Then the page shows, in order:
      | role    | name                |
      | heading | Example Application |
      | region  | Example action      |
      | button  | Run example action  |
      | textbox | Example input       |
      | button  | Submit              |

  @Smoke
  Scenario: Run the example action
    When I run the example action
    Then I see the example action result

  @Regression
  Scenario Outline: Submitting <case> shows "<message>"
    When I submit the form with "<value>"
    Then the form message is "<message>"

    Examples:
      | case                  | value                                               | message                              |
      | a valid value         | Sewer main inspection                               | Form submitted successfully          |
      | an empty value        |                                                     | Please enter a value                 |
      | a value over 50 chars | This value is intentionally longer than fifty chars | Value must be 50 characters or fewer |
