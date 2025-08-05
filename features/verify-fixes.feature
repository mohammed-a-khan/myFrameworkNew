@verify @data-driven
Feature: Verify Data-Driven Test Fixes

  @negative-login @data-driven
  Scenario Outline: Failed login with invalid credentials
    Given I am on the OrangeHRM login page
    When I enter username "<username>" and password "<password>"
    And I click the login button
    Then I should see an error message "<errorMessage>"
    And I should remain on the login page

    Examples: {"type": "csv", "source": "testdata/verify-invalid-login.csv"}