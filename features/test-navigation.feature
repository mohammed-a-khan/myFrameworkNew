@navigation
Feature: Test Navigation
  Test basic navigation

  @simple-nav
  Scenario: Navigate to login page
    Given I am on the OrangeHRM application
    Then I should be on the "login" page