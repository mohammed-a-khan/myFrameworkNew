@login @data-driven
Feature: OrangeHRM Login with @CSDataSource
  Demonstrating data-driven testing using @CSDataSource annotation
  Using external data sources (Excel, CSV, JSON)

  @positive @regression @CSDataSource(type="JSON", path="testdata/login_credentials.json")
  Scenario: Login with credentials from external data source
    Given I am on the OrangeHRM login page
    When I enter username "<username>" and password "<password>"
    And I click the login button
    Then I should be redirected to the dashboard
    And I should see "Dashboard" as page title
    And I should see the user dropdown with text "<expectedUser>"

  @negative @smoke @CSDataSource(type="CSV", path="testdata/invalid_credentials.csv")
  Scenario: Failed login attempts from CSV data
    Given I am on the OrangeHRM login page
    When I enter username "<username>" and password "<password>"
    And I click the login button
    Then I should see an error message "<errorMessage>"
    And I should remain on the login page

  @comprehensive @CSDataSource(type="EXCEL", path="testdata/login_test_data.xlsx", sheet="LoginTests")
  Scenario: Comprehensive login testing from Excel
    Given I am on the OrangeHRM login page
    When I enter username "<username>" and password "<password>"
    And I click the login button
    Then the login result should be "<expectedResult>"
    And the page title should contain "<expectedTitle>"