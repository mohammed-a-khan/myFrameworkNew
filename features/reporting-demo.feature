@reporting @demo
Feature: Custom Reporting Demo
  Demonstrates how to use custom logging and reporting in BDD tests

  @logging-example
  Scenario: Login with detailed logging
    Given I log "═══════════════════════════════════════"
    And I log "Starting login test with custom logging"
    And I log "Test Time: 2025-08-07 18:00:00"
    And I log "═══════════════════════════════════════"
    
    When I log "STEP 1: Navigate to login page"
    And I am on the login page
    Then I log "✓ Login page loaded successfully"
    
    When I log "STEP 2: Enter credentials"
    And I enter username "Admin" and password "admin123"
    Then I log "✓ Credentials entered"
    
    When I log "STEP 3: Submit login"
    And I click the login button
    Then I log "✓ Login form submitted"
    
    When I wait for 2 seconds
    Then I should see the dashboard
    And I log "✓ PASS: Login successful - Dashboard displayed"
    And I log "═══════════════════════════════════════"
    And I log "Test completed successfully"
    And I log "═══════════════════════════════════════"

  @data-logging
  Scenario Outline: Login attempts with data logging
    Given I log "Test Data Set: <testCase>"
    And I log "  Username: <username>"
    And I log "  Password: <masked_password>"
    And I log "  Expected: <expected>"
    
    When I am on the login page
    And I login with username "<username>" and password "<password>"
    
    Then I log "Result: <result>"
    And I verify login result is "<expected>"
    
    Examples:
      | testCase | username | password | masked_password | expected | result |
      | TC001    | Admin    | admin123 | ********       | success  | PASS   |
      | TC002    | invalid  | wrong    | *****          | failure  | PASS   |
      | TC003    | ""       | ""       | <empty>        | failure  | PASS   |

  @performance-logging
  Scenario: Login with performance metrics
    Given I log "[PERFORMANCE TEST] Starting performance measurement"
    And I record the start time
    
    When I am on the login page
    And I log "[METRIC] Page load completed"
    And I record the page load time
    
    When I enter username "Admin" and password "admin123"
    And I log "[METRIC] Credentials entry completed"
    
    When I click the login button
    And I wait for dashboard to load
    Then I log "[METRIC] Login process completed"
    And I record the total execution time
    
    And I log "[PERFORMANCE SUMMARY]"
    And I log "  - Page Load Time: 1.2 seconds"
    And I log "  - Login Time: 2.1 seconds"
    And I log "  - Total Time: 3.3 seconds"
    And I log "[RESULT] Performance within acceptable limits"

  @structured-logging
  Scenario: Structured logging example
    # Configuration logging
    Given I log "[CONFIG] Test Environment: QA"
    And I log "[CONFIG] Browser: Chrome"
    And I log "[CONFIG] Mode: Sequential"
    
    # Test execution
    When I log "[TEST_START] Structured logging demo"
    And I log "[ACTION] Navigate to application"
    And I am on the login page
    Then I log "[CHECKPOINT] Login page displayed"
    
    When I log "[ACTION] Authenticate user"
    And I login with username "Admin" and password "admin123"
    Then I log "[CHECKPOINT] Authentication successful"
    
    And I log "[TEST_END] All validations passed"

  @warning-error-demo
  Scenario: Demonstrating warnings and error logging
    Given I am on the login page
    And I log "ℹ️ INFO: Starting login validation test"
    
    When I log "⚠️ WARNING: Using test environment - data may be limited"
    And I enter username "" and password ""
    And I click the login button
    
    Then I should see an error message "Invalid credentials"
    And I log "❌ ERROR: Login failed with empty credentials (expected)"
    And I log "✅ SUCCESS: Error handling working correctly"
    
    When I log "⚠️ WARNING: Retry attempt with valid credentials"
    And I login with username "Admin" and password "admin123"
    Then I should see the dashboard
    And I log "✅ SUCCESS: Login successful after retry"