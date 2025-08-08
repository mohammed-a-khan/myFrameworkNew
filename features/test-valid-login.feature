@test
Feature: Test Valid Login
  Test valid login only

  @valid-login
  Scenario: Valid login test
    Given I am on the login page
    When I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should see the dashboard