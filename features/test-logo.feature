@logotest
Feature: Test Logo Integration

  Scenario: Generate Report with Logo
    Given I am on the login page
    Then I log "Testing logo integration in report"
    And I take a screenshot "logo_test_screenshot"
