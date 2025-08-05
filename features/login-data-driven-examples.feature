@login @data-driven
Feature: OrangeHRM Login with JSON-Configured Data Sources
  Demonstrating data-driven testing using JSON configuration in Examples
  Using external data sources (Excel, CSV, JSON) with dynamic data loading

  @positive @regression @excel-data @data-driven
  Scenario Outline: Login tests with Excel data source
    Given I am on the OrangeHRM login page
    When I enter username "<username>" and password "<password>"
    And I click the login button
    Then I should be redirected to the dashboard
    And I should see "Dashboard" as page title
    And I should see the user dropdown with text "<expectedUser>"

    Examples: {"type": "csv", "source": "testdata/users.csv"}

  @negative @smoke @csv-data @data-driven
  Scenario Outline: Failed login tests with CSV data source
    Given I am on the OrangeHRM login page
    When I enter username "<username>" and password "<password>"
    And I click the login button
    Then I should see an error message "<errorMessage>"
    And I should remain on the login page

    Examples: {"type": "csv", "source": "testdata/invalid_credentials.csv"}

  @comprehensive @json-data @data-driven
  Scenario Outline: Login validation with JSON data source
    Given I am on the OrangeHRM login page
    When I enter username "<username>" and password "<password>"
    And I click the login button
    Then I should be redirected to the dashboard
    And I should see the user dropdown with text "<expectedUser>"

    Examples: {"type": "json", "source": "testdata/login_credentials.json", "path": "$.testData[*]"}