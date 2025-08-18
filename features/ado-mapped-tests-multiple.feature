@ado-integration @TestPlanId:417 @TestSuiteId:418
Feature: Azure DevOps Mapped Tests
  Tests specifically mapped to Azure DevOps test cases for demonstration
  These tests will update test results in Azure DevOps Test Plan 417, Test Suite 418

  @login @smoke @TestCaseId:{419,420}
  Scenario: ADO Test Case 419 - Valid Login Test
    Given I am on the OrangeHRM login page
    When I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should see the dashboard page
    And the dashboard header should display "Dashboard"
    And I take a screenshot "ado_test_419_success"
