@debug @data-driven
Feature: Debug Data Substitution

  @csv-test @data-driven
  Scenario Outline: Test CSV data substitution
    Given I am on the OrangeHRM login page
    When I enter username "<username>" and password "<password>"
    Then I should see an error message "<errorMessage>"

    Examples: {"type": "csv", "source": "testdata/valid_users.csv"}