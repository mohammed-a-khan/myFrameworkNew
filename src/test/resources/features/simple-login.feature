@login @smoke
Feature: OrangeHRM Login Functionality
  As a user of OrangeHRM
  I want to be able to login to the system
  So that I can access the dashboard

  @positive @sequential
  Scenario: Successful login with valid credentials
    Given I am on the OrangeHRM login page
    When I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should be redirected to the dashboard
    And I should see "Dashboard" as page title
    And I should see the user dropdown with text "kamrul user"

  @negative @sequential
  Scenario: Failed login with invalid credentials
    Given I am on the OrangeHRM login page
    When I enter username "invalid" and password "wrongpass"
    And I click the login button
    Then I should see an error message "Invalid credentials"
    And I should remain on the login page