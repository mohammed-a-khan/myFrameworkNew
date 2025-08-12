Feature: Debug Login Test
  Test to diagnose login issues

  Scenario: Debug Valid Login
    Given I am on the OrangeHRM login page
    When I enter username "Admin" and password "admin123"
    And I click the login button
    And I wait for "5" seconds
    And I take a screenshot "debug_after_login"
    Then I log the current URL

  Scenario: Debug Invalid Login
    Given I am on the OrangeHRM login page
    When I enter username "invalid" and password "wrongpassword"
    And I click the login button
    And I wait for "5" seconds
    And I take a screenshot "debug_after_invalid_login"
    Then I log the current URL