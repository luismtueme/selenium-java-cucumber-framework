@ui @authenticated @Regression
Feature: Items page
  Scenarios tagged @authenticated start logged in: the session is saved once per
  worker and reused, so they skip the login page. Items created here are
  deleted after each scenario.

  @Smoke
  Scenario: The items page opens without logging in again
    Given I am on the items page
    Then I see the items page

  Scenario: An item added through the UI is saved
    Given I am on the items page
    When I add an item named "Catch basin 12 inspection" on the items page
    Then the items list shows "Catch basin 12 inspection"
    And the API returns the item added on the page

  Scenario: An item created through the API is listed
    Given an item named "Outfall 3 inspection" exists
    When I am on the items page
    Then the items list shows "Outfall 3 inspection"
