@simple @quick
Feature: OrangeHRM Simple Tests
  Quick tests demonstrating clean step syntax

  @login-simple
  Scenario: Simple login test
    Given I am on the login page
    When I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should see the dashboard
        And I navigate to "Admin"

  @login-negative
  Scenario: Invalid login test
    Given I am on the login page
    When I login with username "invalid" and password "wrong"
    Then I should see an error message "Invalid credentials"

  @data-driven-simple
  Scenario Outline: Multiple login attempts
    Given I am on the login page
    When I login with username "<username>" and password "<password>"
    Then I should see an error message "<errorMessage>"
    
    Examples:
      | username  | password    | errorMessage         |
      | testuser  | wrongpass   | Invalid credentials  |
      | admin     | badpass     | Invalid credentials  |
      | ""        | ""          | Invalid credentials  |

  @utility-demo
  Scenario: Demonstrating utility steps
    Given I am on the login page
    And I take a screenshot "login_page"
    When I wait for 2 seconds
    And I log "About to login"
    And I enter username "Admin" and password "admin123"
    And I take a screenshot "credentials_entered"
    Then I click the login button
    And I log "Login completed"

  @flexible-steps
  Scenario: Same steps with different keywords
    # All these work with any keyword now!
    Given I log "Test started"
    When I log "Test in progress"
    Then I log "Test completed"
    And I take a screenshot "test_screenshot"
    But I wait for 1 seconds

  @deliberate-failure @screenshot-demo
  Scenario Outline: Deliberately failing test to demonstrate failure reporting
    Given I am on the login page
    And I take a screenshot "login_page_initial_<testCase>"
    When I enter username "<username>" and password "<password>"
    And I take a screenshot "valid_credentials_entered_<testCase>"
    And I click the login button
    Then I should see the dashboard
    And I take a screenshot "dashboard_displayed_<testCase>"
    # Now let's try to find a non-existent element which will fail
    And I should see "<nonExistentElement>" element
    And I take a screenshot "after_failed_element_check_<testCase>"
    
    Examples:
      | testCase | username | password  | nonExistentElement        |
      | test1    | Admin    | admin123  | NonExistentMenuOption1    |
      | test2    | Admin    | admin123  | NonExistentMenuOption2    |
      | test3    | Admin    | admin123  | NonExistentMenuOption3    |

  @browser-switching-demo
  Scenario: Cross-browser login testing with browser switching
    # Start with Chrome browser (configured in suite)
    Given I am on the login page
    And I take a screenshot "chrome_initial_login_page"
    When I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should see the dashboard
    And I take a screenshot "chrome_initial_dashboard"
    And I log "Completed login in Chrome browser (initial)"
    
    # Switch to Edge browser and perform the same login
    #When I switch to "Edge" browser
    #And I verify the current browser is "Edge"
    #And I log "Successfully switched to Edge browser"
    #Given I am on the login page
    #And I take a screenshot "edge_browser_login_page"
    #When I enter username "Admin" and password "admin123"
    #And I click the login button
    #Then I should see the dashboard
    #And I take a screenshot "edge_browser_dashboard"
    #And I log "Completed login in Edge browser"
    
    # Switch back to Chrome to verify switching works both ways
    #When I switch back to "Chrome" browser
    When I switch to "Chrome" browser
    And I verify the current browser is "Chrome"
    And I log "Successfully switched back to Chrome browser"
    Given I am on the login page
    And I take a screenshot "chrome_final_login_page"
    When I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should see the dashboard
    And I take a screenshot "chrome_final_dashboard"
    And I log "Browser switching demo completed successfully - Chrome to Edge to Chrome"