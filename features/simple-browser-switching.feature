@simple-browser-switching
Feature: Simple Browser Switching Test
  Validate basic browser switching functionality

  @basic-switching
  Scenario: Basic browser switching test
    Given I am using "Chrome" browser
    And I verify the current browser is "Chrome"
    When I switch to "Edge" browser
    Then I verify the current browser is "Edge"
    When I switch back to "Chrome" browser
    Then I verify the current browser is "Chrome"