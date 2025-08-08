@orangehrm @basic
Feature: OrangeHRM Basic Login Test
  Simple test to verify login functionality

  @login-basic
  Scenario: Basic login test
    Given I am on the OrangeHRM application
    When I login with username "Admin" and password "admin123"
    Then I should see the dashboard