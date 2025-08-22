@simple
Feature: Simple Test
  Basic test to verify ADO override functionality

  @login
  Scenario: Simple login test
    Given I am on the OrangeHRM application
    When I login with username "Admin" and password "admin123"
    Then I should see the dashboard