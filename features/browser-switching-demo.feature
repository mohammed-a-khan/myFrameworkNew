@browser-switching
Feature: Browser Switching Demo
  Demonstrate seamless browser switching during test execution

  @chrome-to-edge
  Scenario: Test cross-browser functionality
    Given I am using "Chrome" browser
    When I navigate to the login page
    And I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should see the dashboard
    
    # Switch to Edge browser for legacy testing
    When I switch to "Edge" browser
    And I navigate to the login page
    And I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should see the dashboard
    And I verify the current browser is "Edge"
    
    # Switch back to Chrome
    When I switch back to "Chrome" browser
    And I navigate to the login page
    Then I verify the current browser is "Chrome"

  @firefox-testing
  Scenario: Firefox browser testing
    Given I am using "Firefox" browser
    When I navigate to the login page
    And I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should see the dashboard
    And I verify the current browser is "Firefox"

  @multi-browser-validation
  Scenario Outline: Multi-browser validation
    Given I am using "<browser>" browser
    When I navigate to the login page
    And I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should see the dashboard
    And I verify the current browser is "<browser>"
    
    Examples:
      | browser |
      | Chrome  |
      | Edge    |
      | Firefox |