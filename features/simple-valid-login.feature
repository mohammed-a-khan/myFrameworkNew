@valid
Feature: Simple Valid Login
  Test valid login only

  @login-valid
  Scenario: Valid login test
    Given I am on the login page
    When I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should see the dashboard