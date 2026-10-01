@api @Regression
Feature: Items API
  API scenarios run without a browser. Every request and response is attached
  to the Cucumber report, with passwords and tokens masked.

  @Smoke
  Scenario: Create an item
    Given I am authenticated with the API
    When I create an item named "Manhole 42 inspection"
    Then the response status is 201
    And the response body matches:
      """json
      { "name": "Manhole 42 inspection" }
      """
    And the created item can be fetched by its id

  Scenario: An item without a name is rejected
    Given I am authenticated with the API
    When I create an item named ""
    Then the response status is 400
    And the response body matches:
      """json
      { "error": { "code": "VALIDATION_ERROR", "field": "name" } }
      """

  Scenario: Requests without a token are rejected
    When I list the items without authenticating
    Then the response status is 401
