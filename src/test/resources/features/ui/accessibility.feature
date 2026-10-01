@ui @a11y @Regression
Feature: Accessibility
  Every page meets WCAG 2.1 A and AA (checked with axe-core). Violations are
  attached to the report with the failing elements and a link explaining the fix.
  Add a row for each new page.

  Scenario Outline: The <page> page has no accessibility violations
    Given I open the <page> page
    Then the page has no accessibility violations

    Examples: Public pages
      | page  |
      | form  |
      | login |

    @authenticated
    Examples: Pages that need a login
      | page  |
      | items |
