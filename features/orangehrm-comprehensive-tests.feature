@comprehensive @orangehrm
Feature: OrangeHRM Comprehensive Tests
  Demonstrating all CS TestForge capabilities with clean syntax

  Background:
    Given I maximize the browser window
    And I clear browser cache

  # ========== BASIC LOGIN SCENARIOS ==========
  
  @smoke @login
  Scenario: Successful login with valid credentials
    Given I am on the login page
    When I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should see the dashboard
    And I take a screenshot "successful_login"

  @negative @login
  Scenario: Failed login with invalid credentials
    Given I am on the OrangeHRM application
    When I login with username "invalid" and password "wrong"
    Then I should see an error message "Invalid credentials"
    And I log "Login failed as expected"

  @navigation
  Scenario: Navigate between pages
    Given I am logged in as "admin"
    When I am on the "dashboard" page
    Then I should be on the "dashboard" page
    And I refresh the page
    And I wait for 2 seconds
    Then I should see the dashboard

  # ========== DATA-DRIVEN SCENARIOS ==========
  
  @data-driven @csv
  Scenario Outline: Login with multiple users from CSV
    Given I am on the login page
    When I enter username "<username>" and password "<password>"
    And I click the login button
    Then I should see an error message "Invalid credentials"
    
    Examples: {"type": "csv", "source": "testdata/login-test-data.csv"}

  @data-driven @excel
  Scenario Outline: Login with Excel data source
    Given I log "Testing with user: <username>"
    And I am on the login page
    When I login with username "<username>" and password "<password>"
    Then I should see an error message "Invalid credentials"
    And I take a screenshot "login_<username>"
    
    Examples: {"type": "excel", "source": "testdata/TestData.xlsx", "sheet": "LoginData"}

  @data-driven @json
  Scenario Outline: Login with JSON data source
    Given I am on the OrangeHRM application
    When I enter username "<username>" and password "<password>"
    And I click the "Login" button
    Then I should see an error message "<expectedError>"
    
    Examples: {"type": "json", "source": "testdata/login-data.json", "path": "$.invalidLogins[*]"}

  # ========== DATA ROW ACCESS SCENARIOS ==========
  
  @datarow @complete
  Scenario Outline: Login with complete data row access
    Given I am on the login page
    When I login with test data
    Then I verify all user data
    
    Examples:
      | username | password  | expectedUser | department | role    |
      | Admin    | admin123  | Paul Smith   | Admin      | ESS     |
      | user1    | user123   | John Doe     | HR         | User    |

  @datarow @metadata
  Scenario Outline: Login with metadata access
    Given I am on the login page
    When I verify login with complete data set
    Then I log "Test completed with data source"
    
    Examples: {"type": "csv", "source": "testdata/login-test-data.csv"}

  @datarow @helper
  Scenario Outline: Login using helper methods
    Given I am on the login page
    When I perform a complete login test
    Then I take a screenshot "test_<username>"
    
    Examples:
      | username | password | expectedResult | errorMessage      |
      | Admin    | admin123 | success        |                   |
      | invalid  | wrong    | failure        | Invalid credentials |

  # ========== DATA TABLE SCENARIOS ==========
  
  @datatable @vertical
  Scenario: Login with data table (vertical format)
    Given I am on the login page
    When I enter the following credentials:
      | field    | value     |
      | username | Admin     |
      | password | admin123  |
    And I click the login button
    Then I should see the dashboard

  @datatable @multiple
  Scenario: Verify multiple users can login
    Given I verify the following users can login:
      | username | password  | expectedResult |
      | Admin    | admin123  | success        |
      | invalid  | wrong     | failure        |
      | user1    | user123   | success        |
    Then I log "Multiple user verification completed"

  # ========== FLEXIBLE STEP USAGE ==========
  
  @flexible
  Scenario: Demonstrating flexible step usage
    # Same step used with different keywords
    Given I take a screenshot "before_test"
    And I am on the login page
    When I take a screenshot "at_login_page"
    And I wait for 1 seconds
    Then I take a screenshot "after_wait"
    
    # Navigation step used flexibly
    Given I am on the "login" page
    When I am on the "login" page
    Then I am on the "login" page
    
    # Utility steps anywhere
    Given I log "Starting test"
    When I log "Executing test"
    Then I log "Test completed"

  # ========== COMPLEX SCENARIO ==========
  
  @complex @e2e
  Scenario Outline: End-to-end login test with all features
    # Utility steps
    Given I log "Testing user: <username>"
    And I take a screenshot "start_<username>"
    
    # Navigation
    Given I am on the OrangeHRM application
    And I wait for 1 seconds
    
    # Login action
    When I enter username "<username>" and password "<password>"
    And I take a screenshot "credentials_entered_<username>"
    And I click the login button
    And I wait for 2 seconds
    
    # Verification based on expected result
    Then I should see the dashboard
    And I verify all user data
    And I take a screenshot "final_<username>"
    
    # Cleanup
    And I clear browser cache
    And I log "Test completed for <username>"
    
    Examples:
      | username | password | expectedUser | role  | department |
      | Admin    | admin123 | Paul Smith   | Admin | IT         |