@ui @authenticated @Regression
Feature: Session idle timeout
  The items page asks the user to log in again after 15 minutes without a click
  or keypress. The browser clock is controlled by the test, so 15 minutes pass
  instantly and the boundary is exact.

  Background:
    Given the browser clock is under test control
    And I am on the items page

  Scenario: The session expires after 15 idle minutes
    When 14 minutes pass without activity
    Then the session is still active
    When 1 minute passes without activity
    Then the session has expired

  Scenario: Activity restarts the idle timer
    When 10 minutes pass without activity
    And I type in the new item name
    And 10 minutes pass without activity
    Then the session is still active
    When 5 minutes pass without activity
    Then the session has expired
