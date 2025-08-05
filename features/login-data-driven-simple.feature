@login @data-driven
Feature: OrangeHRM Login with Simple Scenario Outline
  Demonstrating data-driven testing using Scenario Outline with inline Examples

  @positive @regression
  Scenario Outline: Successful login with valid credentials
    Given I am on the OrangeHRM login page
    When I enter username "<username>" and password "<password>"
    And I click the login button
    Then I should be redirected to the dashboard
    And I should see "Dashboard" as page title
    And I should see the user dropdown with text "<expectedUser>"

    Examples:
      | username | password | expectedUser   |
      | Admin    | admin123 | manda user     |
      | admin    | admin123 | manda user     |

  @negative @smoke
  Scenario Outline: Failed login with invalid credentials
    Given I am on the OrangeHRM login page
    When I enter username "<username>" and password "<password>"
    And I click the login button
    Then I should see an error message "Invalid credentials"
    And I should remain on the login page

    Examples:
      | username  | password    |
      | invalid   | wrongpass   |
      | Admin     | wrongpass   |
      | test123   | test123     |