@smoke @regression
Feature: Example Feature with Optional ADO Integration
  This feature shows how ANY test can optionally include ADO tags
  ADO integration is automatic when ado.enabled=true in config

  @TestCaseId:500 @login
  Scenario: Test mapped to ADO Test Case 500
    Given I am on the OrangeHRM login page
    When I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should see the dashboard page

  @critical
  Scenario: Test WITHOUT ADO mapping (runs normally)
    Given I am on the OrangeHRM login page
    When I enter username "invalid" and password "wrong"
    And I click the login button
    Then I should see an error message containing "Invalid credentials"

  @TestCaseId:501 @TestPlanId:600 @TestSuiteId:601
  Scenario: Test with full ADO configuration
    Given I am on the OrangeHRM login page
    When I navigate to the PIM module
    Then I should see the employee list

# Notes:
# - If ado.enabled=false, tests run normally without ADO publishing
# - If ado.enabled=true and test has @TestCaseId tag, results go to ADO
# - Tests without @TestCaseId tags run normally even when ADO is enabled
# - You can override Test Plan/Suite with tags or use defaults from config