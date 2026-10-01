@api @db @Regression
Feature: Items are persisted in the database
  Checks what the API wrote, directly in MySQL. These scenarios run only when a
  database is configured (DB_HOST); CI provides one.

  Background:
    Given I am authenticated with the API

  Scenario: A created item is stored
    When I create an item named "Lateral 7 inspection"
    Then the database has the created item named "Lateral 7 inspection"

  Scenario: A deleted item is removed
    Given I create an item named "Temporary item"
    When I delete the created item
    Then the response status is 204
    And the database no longer has the created item
