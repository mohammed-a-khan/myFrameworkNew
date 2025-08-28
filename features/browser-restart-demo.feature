@browser-restart @simple
Feature: Browser Restart Demo
  As a test automation engineer
  I want to restart browsers during test execution
  So that I can test with fresh sessions and different credentials

  @chrome-restart-demo
  Scenario: Demonstrate Chrome browser restart functionality
    Given I am on the login page
    And I take a screenshot "chrome_initial_login"
    When I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should see the dashboard
    And I take a screenshot "chrome_initial_dashboard"
    And I log "Logged in with Admin credentials"
    
    # Restart the same Chrome browser for fresh session
    When I restart the current browser
    And I log "Browser restarted - fresh session started"
    
    # Login with different credentials in the fresh browser
    Given I am on the login page
    And I take a screenshot "chrome_fresh_login"
    When I enter username "Admin" and password "admin123"  
    And I click the login button
    Then I should see the dashboard
    And I take a screenshot "chrome_fresh_dashboard"
    And I log "Successfully logged in after browser restart"

  @multiple-browser-restart
  Scenario: Restart specific browser types
    Given I am on the login page
    And I take a screenshot "initial_chrome_login"
    When I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should see the dashboard
    And I log "Initial Chrome login completed"
    
    # Switch to Edge browser
    When I switch to "Edge" browser
    And I log "Switched to Edge browser"
    Given I am on the login page
    And I take a screenshot "edge_login"
    When I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should see the dashboard
    And I log "Edge login completed"
    
    # Restart Edge browser (force restart same type)
    When I force restart "Edge" browser
    And I log "Edge browser restarted - fresh session"
    Given I am on the login page
    And I take a screenshot "edge_restarted_login"
    When I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should see the dashboard
    And I log "Edge restart login completed"
    
    # Switch back to Chrome (will restart Chrome since Edge was last active)
    When I switch to "Chrome" browser
    And I log "Switched back to Chrome browser"
    Given I am on the login page
    And I take a screenshot "chrome_final_login"
    When I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should see the dashboard
    And I log "Final Chrome login completed"