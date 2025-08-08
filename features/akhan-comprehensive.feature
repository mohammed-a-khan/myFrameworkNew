@akhan @comprehensive
Feature: Akhan Application - Comprehensive Test Suite
  Comprehensive test demonstrating all CS TestForge Framework features

  Background:
    Given I log "═══════════════════════════════════════════"
    And I log "Starting Akhan Application Test"
    And I log "Environment: SIT"
    And I log "═══════════════════════════════════════════"
    And I am on the login page
    And I take a screenshot "login_page_initial"

  @login @reporting
  Scenario: Login with detailed reporting
    Given I log "TEST: Login Functionality"
    When I log "Step 1: Enter credentials"
    And I enter username "testuser" and password "testpass"
    And I take a screenshot "credentials_entered"
    And I log pass "Credentials entered successfully"
    
    When I log "Step 2: Submit login"
    And I click the log on button
    And I wait for 2 seconds
    
    Then I log "Step 3: Verify login success"
    And I should see the home header
    And I log pass "Login successful - Home page displayed"
    And I should see welcome message for user "testuser"
    And I take a screenshot "login_success"
    And I log "═══════════════════════════════════════════"
    And I log "Login test completed successfully"
    And I log "═══════════════════════════════════════════"

  @navigation @smoke
  Scenario: Verify navigation menu with performance tracking
    Given I record the start time
    And I am logged in as "testuser"
    And I record the page load time
    
    When I log "[PERFORMANCE] Verifying navigation menu items"
    Then I should see the following menu items:
      | menuItem               |
      | Home                   |
      | ESSS/Series           |
      | Reference Interests    |
      | Interest History       |
      | External Interests     |
      | System Admin          |
      | Version Information    |
      | File Upload           |
    
    And I record the total execution time
    And I log "[PERFORMANCE] Menu verification completed"

  @module-navigation @parallel
  Scenario Outline: Navigate to <menuItem> module
    Given I am logged in as "testuser"
    And I log "Navigating to <menuItem>"
    
    When I click on "<menuItem>" menu item
    And I wait for page to load
    
    Then I should see the "<pageHeader>" page header
    And I take a screenshot "<menuItem>_page"
    And I log pass "<menuItem> navigation successful"

    Examples:
      | menuItem               | pageHeader             |
      | ESSS/Series           | ESSSs/Series          |
      | Reference Interests    | Reference Interests    |
      | Interest History       | Interest History       |
      | External Interests     | External Interests     |
      | System Admin          | System Admin          |
      | Version Information    | Version Information    |

  @file-upload @special-case
  Scenario: File Upload module navigation
    Given I am logged in as "testuser"
    When I click on "File Upload" menu item
    Then I should see the "Add files" span element
    And I log pass "File Upload page verified with span element"

  @esss-search @data-driven @csv
  Scenario Outline: Search for ESSS with data from CSV
    Given I am logged in as "testuser"
    When I click on "ESSS/Series" menu item
    Then I should see the "ESSSs/Series" page header
    
    # Type dropdown verification and selection
    When I click on the Type dropdown
    And I verify Type dropdown has 9 options
    And I select "<searchType>" from Type dropdown
    
    # Attribute dropdown verification and selection
    When I click on the Attribute dropdown
    And I verify Attribute dropdown options for "<searchType>"
    And I select "<attribute>" from Attribute dropdown
    
    # Search execution
    When I enter "<searchValue>" in search field
    And I click the search button
    And I wait for search results
    
    # Results verification
    Then I should see search results in the table
    And I verify search results contain "<expectedResult>"
    And I log pass "Search completed for <searchType> - <attribute>: <searchValue>"

    @CSDataSource(type="csv", source="resources/testdata/esss-search-data.csv")
    Examples:
      | searchType | attribute | searchValue  | expectedResult |
      | ESSS      | Key       | MESA 2001-5  | MESA 2001-5   |

  @esss-search-json @data-driven
  Scenario: Search for ESSS using JSON test data
    Given I am logged in as "testuser"
    And I log "[DATA-DRIVEN] Using JSON test data"
    
    When I click on "ESSS/Series" menu item
    Then I should see the "ESSSs/Series" page header
    
    # Detailed Type dropdown verification
    When I log "Verifying Type dropdown options"
    And I click on the Type dropdown
    Then I should see the following Type options:
      | option                |
      | ESSS                 |
      | Series               |
      | Reference Interest   |
      | Fallback Interest    |
      | Product Group        |
      | Business Line        |
      | Benchmark           |
      | Administrator        |
      | CDI Name            |
    And I log pass "All 9 Type options verified"
    
    When I select "ESSS" from Type dropdown
    And I log "Selected: ESSS"
    
    # Detailed Attribute dropdown verification
    When I log "Verifying Attribute dropdown options for ESSS"
    And I click on the Attribute dropdown
    Then I should see the following Attribute options:
      | option |
      | Key    |
      | Name   |
      | ID     |
    And I log pass "All 3 Attribute options verified for ESSS"
    
    When I select "Key" from Attribute dropdown
    And I log "Selected: Key"
    
    # Search with test data
    When I log "Entering ESSS key from test data"
    And I enter ESSS key from test data
    And I take a screenshot "esss_search_input"
    And I click the search button
    And I wait for 3 seconds
    
    # Results verification with detailed logging
    Then I log "Verifying search results"
    And I should see search results in the table
    And I log the search results summary
    And I should find ESSS with the entered key in the results
    And I take a screenshot "esss_search_results"
    And I log pass "ESSS search completed successfully"

  @esss-advanced @performance
  Scenario: Advanced ESSS search with performance metrics
    Given I am logged in as "testuser"
    And I record the start time
    
    When I navigate to ESSS/Series module with performance tracking
    And I perform ESSS search with performance metrics
    
    Then I verify search performance is within limits
    And I log "[PERFORMANCE SUMMARY]"
    And I log "  - Page Load: < 2 seconds"
    And I log "  - Search Execution: < 3 seconds"
    And I log "  - Total Time: < 5 seconds"

  @error-handling
  Scenario: Login with invalid credentials
    Given I log warning "Testing error handling"
    When I enter username "invalid" and password "wrong"
    And I click the log on button
    Then I should see login error
    And I log fail "Login failed as expected with invalid credentials"
    And I take a screenshot "login_error"

  @cleanup
  Scenario: Logout from application
    Given I am logged in as "testuser"
    When I perform logout
    Then I should be on login page
    And I log pass "Logout successful"
    And I log "═══════════════════════════════════════════"
    And I log "All tests completed"
    And I log "═══════════════════════════════════════════"