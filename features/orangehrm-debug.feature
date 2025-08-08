@debug
Feature: Debug OrangeHRM Login

  @debug-login
  Scenario: Debug login test
    Given I am on the OrangeHRM application
    When I login with username "Admin" and password "admin123"
    Then I take a screenshot "after_login"