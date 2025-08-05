@login @smoke @ado-integrated
@CSFeature(
  name = "OrangeHRM Login Functionality",
  epic = "Authentication Module",
  author = "QA Team",
  tags = {"login", "authentication", "core"}
)
Feature: OrangeHRM Login Functionality
  As a user of OrangeHRM
  I want to be able to login to the system
  So that I can access the dashboard

  @positive @sequential @ado-testcase-1001
  @CSTest(
    name = "Valid Login Test",
    description = "Verify successful login with valid credentials",
    category = "Smoke",
    priority = "High",
    testId = "TC-1001",
    requirementId = "REQ-AUTH-001"
  )
  @CSJiraTicket(value = "JIRA-1234")
  Scenario: Successful login with valid credentials
    Given I am on the OrangeHRM login page
    When I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should be redirected to the dashboard
    And I should see "Dashboard" as page title
    And I should see the user dropdown with text "manda user"

  @negative @sequential @ado-testcase-1002
  @CSTest(
    name = "Invalid Login Test",
    description = "Verify login fails with invalid credentials",
    category = "Negative",
    priority = "High",
    testId = "TC-1002",
    requirementId = "REQ-AUTH-002"
  )
  @CSRetry(count = 2, delay = 1000)
  Scenario: Failed login with invalid credentials
    Given I am on the OrangeHRM login page
    When I enter username "invalid" and password "wrongpass"
    And I click the login button
    Then I should see an error message "Invalid credentials"
    And I should remain on the login page