@akhan @framework-demo @azure-devops
Feature: Akhan Application Test
  Demonstrates all CS TestForge Framework features
  Using exact XPaths and requirements
  
  # Azure DevOps Configuration
  @ADO-TestPlan:12345
  @ADO-TestSuite:67890
  @ADO-Organization:testforge
  @ADO-Project:CSTestForge
  
  Background:
    Given I am on the Akhan application
  
  @login @encryption @TestCaseId:{23232,42323}
  Scenario: Login with encrypted credentials (validates multiple test cases)
    When I enter username "testuser1"
    And I enter password from encrypted source
    And I click the login button
    Then I should be on the home page
    And I take a screenshot named "login-success"
  
  @navigation @dynamic-xpath @ADO-TestCase:{TC-002,TC-004,TC-005}
  Scenario: Navigate using dynamic menu XPath
    Given I am logged in
    When I navigate to "ESSS/Series"
    Then I should be on the ESSS page
    When I navigate to "Reference Interests"
    And I navigate to "Interest History"
    And I navigate to "Home"
    Then I should be on the home page
  
  @search @data-driven @csv-source @ADO-TestCase:TC-003
  Scenario Outline: ESSS search with CSV data source
    Given I am logged in
    When I navigate to "ESSS/Series"
    And I select "<SearchType>" from search type dropdown
    And I select "<Attribute>" from attribute dropdown
    And I click search
    Then I should see <ExpectedResult>
    
    Examples: {"type": "csv", "source": "testdata/akhan-search-data.csv"}
  
  @login @json-source @ADO-TestCase:TC-004
  Scenario Outline: Login with JSON data source
    When I enter username "<username>"
    And I enter password from encrypted source
    And I click the login button
    Then I should be on the home page
    
    Examples: {"type": "json", "source": "testdata/akhan-users.json"}
  
  @search @excel-source @ADO-TestCase:TC-005
  Scenario Outline: ESSS search with Excel data
    Given I am logged in
    When I navigate to "ESSS/Series"
    And I select "<SearchType>" from search type dropdown
    And I select "<Attribute>" from attribute dropdown
    And I click search
    Then I should see <ExpectedResult>
    
    Examples: {"type": "excel", "source": "testdata/AkhanTestData.xlsx", "sheet": "SearchData"}
  
  @navigation @data-driven @ADO-TestCase:TC-006
  Scenario Outline: Navigation with response time validation
    Given I am logged in
    When I navigate to "<MenuItem>"
    Then the page should load successfully
    And response time should be less than <MaxResponseTime> ms
    
    Examples:
      | MenuItem            | MaxResponseTime |
      | ESSS/Series        | 3000            |
      | Reference Interests| 2500            |
      | Interest History   | 2500            |
      | External Interests | 2500            |
      | System Admin       | 2500            |
      | Version Information| 2000            |
      | File Upload        | 3000            |
  
  @datarow @complete @ADO-TestCase:TC-007
  Scenario Outline: Login with complete data row access
    When I login with username "<username>" and encrypted password
    Then I should be on the home page
    And I verify user role is "<role>"
    And I verify user department is "<department>"
    
    Examples:
      | username  | password | role      | department | accessLevel |
      | testuser1 | test123  | admin     | IT         | full        |
      | testuser2 | test456  | user      | HR         | read        |
      | testuser3 | test789  | superuser | Admin      | admin       |
  
  @datarow @metadata @ADO-TestCase:TC-008
  Scenario Outline: Search with data row metadata access
    Given I am logged in
    When I navigate to "ESSS/Series"
    And I perform search with type "<SearchType>" and attribute "<Attribute>"
    Then I should see search results
    And I verify data source metadata
    
    Examples: {"type": "csv", "source": "testdata/akhan-search-data.csv"}
  
  @e2e @comprehensive @ADO-TestCase:TC-009
  Scenario: Complete workflow with all features
    # Login with encryption
    When I login with username "testuser1" and encrypted password
    Then I should be on the home page
    
    # Navigate to ESSS/Series
    When I navigate to "ESSS/Series"
    Then I should be on the ESSS page
    
    # Perform search
    When I select "ESSS" from search type dropdown
    And I select "Active" from attribute dropdown
    And I click search
    Then I should see search results
    
    # Navigate through menu items
    When I navigate to "Reference Interests"
    And I navigate to "Interest History"
    And I navigate to "External Interests"
    And I navigate to "Home"
    Then I should be on the home page
    
    # Logout
    When I logout
    Then I should be on the login page
  
  @parallel @multi-user @ADO-TestCase:TC-010
  Scenario Outline: Multi-user parallel execution
    When I login with username "<username>" and encrypted password
    And I navigate to "<module>"
    And I perform search with type "<searchType>" and attribute "<attribute>"
    Then I should see <expectedResult>
    And I logout
    
    Examples:
      | username  | module      | searchType | attribute | expectedResult |
      | testuser1 | ESSS/Series | ESSS       | Active    | search results |
      | testuser2 | ESSS/Series | Series     | Inactive  | search results |
      | testuser3 | ESSS/Series | ESSS       | Pending   | no results     |
  
  @performance @metrics @ADO-TestCase:TC-011
  Scenario: Performance testing with metrics
    Given I am logged in
    When I navigate to "ESSS/Series"
    And I perform search with type "ESSS" and attribute "Active"
    Then search time should be less than 2000 ms
    And I verify search results
    And performance metrics should be captured in report
  
  @retry @flaky-detection @ADO-TestCase:TC-012
  Scenario: Search with retry on failure
    Given I am logged in
    When I navigate to "ESSS/Series"
    And I perform search with type "Series" and attribute "Active"
    Then I should see search results
    And retry metrics should be captured if test was flaky
  
  @validation @negative @ADO-TestCase:TC-013
  Scenario Outline: Negative test cases
    When I enter username "<username>"
    And I enter password from encrypted source
    And I click the login button
    Then I should see error message "<errorMessage>"
    
    Examples:
      | username   | errorMessage                |
      | ""         | Username is required        |
      | invalid    | Invalid credentials         |
      | blocked    | Account is blocked          |
  
  @custom-reporting @ADO-TestCase:TC-014
  Scenario: Test with custom reporting
    Given I am logged in
    When I navigate to "ESSS/Series"
    And I perform search with type "ESSS" and attribute "Active"
    Then I should see search results
    And custom metrics should be added to report
    And screenshots should be attached to report
    And performance data should be logged