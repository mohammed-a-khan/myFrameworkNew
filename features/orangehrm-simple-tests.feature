@simple @quick
Feature: OrangeHRM Simple Tests
  Quick tests demonstrating clean step syntax

  @login-simple
  Scenario: Simple login test
    Given I am on the login page
    When I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should see the dashboard

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
      | ""        | ""          | Required            |

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