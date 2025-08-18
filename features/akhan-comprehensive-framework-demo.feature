@regression @akhan @framework-demo
Feature: Akhan Application - Comprehensive Framework Demonstration
  This feature demonstrates all CS TestForge Framework capabilities including:
  - BDD/Cucumber integration with tags
  - Page Object Model with CSBasePage
  - CSElement smart element handling
  - Data-driven testing (CSV, JSON, Excel)
  - Configuration management
  - Azure DevOps integration
  - Parallel execution support
  - Failure analysis and recovery
  - Reporting with screenshots
  - Environment-specific testing

  Background:
    Given the test environment is configured for "SIT"
    And browser configuration is set to "chrome" with "headless" mode "false"

  @smoke @TestCaseId-1001 @TestPlanId-100 @TestSuiteId-10
  Scenario: AKH-001 - Login functionality with valid credentials
    Given I navigate to the Akhan application
    When I enter username "testuser" in the login field
    And I enter password "testpass" in the password field
    And I click on the Log On button
    Then I should see the Home page header
    And the welcome message should contain "testuser"
    And I take a screenshot "successful-login"

  @negative @TestCaseId-1002
  Scenario: AKH-002 - Login with invalid credentials and failure handling
    Given I navigate to the Akhan application
    When I enter username "invalid_user" in the login field
    And I enter password "wrong_pass" in the password field
    And I click on the Log On button
    Then login should fail with appropriate error
    And the system should capture failure details for analysis

  @navigation @TestCaseId-1003
  Scenario: AKH-003 - Verify all navigation menu items
    Given I am logged into the Akhan application
    Then I should see the following menu items:
      | MenuItem              |
      | Home                  |
      | ESSS/Series          |
      | Reference Interests   |
      | Interest History      |
      | External Interests    |
      | System Admin         |
      | Version Information   |
      | File Upload          |
    And each menu item should be clickable

  @module-navigation @TestCaseId-1004
  Scenario Outline: AKH-004 - Navigate to different modules
    Given I am logged into the Akhan application
    When I click on the "<Module>" menu item
    Then I should be on the "<Module>" page
    And the page header should be "<ExpectedHeader>"

    Examples:
      | Module               | ExpectedHeader        |
      | ESSS/Series         | ESSSs/Series         |
      | Reference Interests  | Reference Interests   |
      | Interest History     | Interest History      |
      | External Interests   | External Interests    |

  @file-upload @TestCaseId-1005
  Scenario: AKH-005 - Verify File Upload module
    Given I am logged into the Akhan application
    When I click on the "File Upload" menu item
    Then I should see the "Add files" element
    And the file upload functionality should be available

  @data-driven-csv @TestCaseId-1006
  Scenario Outline: AKH-006 - ESSS Search with CSV data
    Given I am logged into the Akhan application
    And I navigate to the ESSS/Series module
    When I select "ESSS" from the Type dropdown
    And I select "Key" from the Attribute dropdown
    And I search for ESSS with key "<ESSKey>"
    Then the search results should contain "<ESSKey>"
    And the result type should be "ESSS"

    Examples: {"type": "csv", "source": "testdata/akhan-esss-data.csv"}

  @data-driven-json @TestCaseId-1007
  Scenario Outline: AKH-007 - ESSS Search with JSON data
    Given I am logged into the Akhan application
    And I navigate to the ESSS/Series module
    When I select "<SearchType>" from the Type dropdown
    And I select "<SearchAttribute>" from the Attribute dropdown
    And I search for "<SearchType>" with "<SearchAttribute>" "<SearchValue>"
    Then the search results should contain "<SearchValue>"
    And the result type should be "<SearchType>"

    Examples: {"type": "json", "source": "testdata/akhan-search-data.json", "path": "$.searchData[*]"}

  @dropdown-validation @TestCaseId-1008
  Scenario: AKH-008 - Validate Type dropdown options
    Given I am logged into the Akhan application
    And I navigate to the ESSS/Series module
    When I click on the Type dropdown
    Then I should see the following Type options:
      | Option               |
      | ESSS                |
      | Series              |
      | Reference Interest   |
      | Fallback Interest   |
      | Product Group       |
      | Business Line       |
      | Benchmark           |
      | Administrator       |
      | CDI Name           |

  @dropdown-dynamic @TestCaseId-1009
  Scenario: AKH-009 - Validate Attribute dropdown dynamic behavior
    Given I am logged into the Akhan application
    And I navigate to the ESSS/Series module
    When I select "ESSS" from the Type dropdown
    Then the Attribute dropdown should show:
      | Attribute |
      | Key       |
      | Name      |
      | ID        |
    When I select "Series" from the Type dropdown
    Then the Attribute dropdown options should change accordingly

  @search-validation @TestCaseId-1010
  Scenario: AKH-010 - Complex ESSS search with result validation
    Given I am logged into the Akhan application
    And I navigate to the ESSS/Series module
    When I perform the following search:
      | Field     | Value        |
      | Type      | ESSS        |
      | Attribute | Key         |
      | Search    | MESA 2001-5 |
    Then I should see search results in the table
    And I validate each result row:
      | Column | Expected Value |
      | 2      | ESSS          |
      | 4      | MESA 2001-5   |

  @performance @TestCaseId-1011
  Scenario: AKH-011 - Performance monitoring for search operations
    Given I am logged into the Akhan application
    And I navigate to the ESSS/Series module
    When I start performance monitoring
    And I search for ESSS with key "MESA 2001-5"
    Then the search should complete within 3 seconds
    And performance metrics should be captured

  @parallel-execution @TestCaseId-1012
  Scenario: AKH-012 - Scenario designed for parallel execution
    Given I am logged into the Akhan application with unique session
    When I perform multiple operations simultaneously
    Then all operations should complete successfully
    And thread isolation should be maintained

  @retry-mechanism @TestCaseId-1013
  Scenario: AKH-013 - Demonstrate retry mechanism on element interaction
    Given I am logged into the Akhan application
    When I interact with a dynamic element that may not be immediately available
    Then the framework should retry the interaction
    And eventually succeed within configured retry attempts

  @screenshot-capture @TestCaseId-1014
  Scenario: AKH-014 - Screenshot capture at various points
    Given I am logged into the Akhan application
    When I navigate through multiple modules
    And I take screenshots at each step:
      | Step                    | Screenshot Name     |
      | Home Page              | home-page          |
      | ESSS Module            | esss-module        |
      | Search Results         | search-results     |
    Then all screenshots should be embedded in the report

  @environment-switching @TestCaseId-1015
  Scenario: AKH-015 - Environment-specific configuration demonstration
    Given the application is configured for "<Environment>" environment
    When I access the application URL
    Then the URL should match the environment configuration
    And environment-specific settings should be applied

    Examples:
      | Environment |
      | SIT        |
      | UAT        |
      | PROD       |