@akhan @framework-demo
Feature: Akhan Application Test
  Demonstrates all CS TestForge Framework features
  Using exact XPaths and requirements
  
  Background:
    Given I am on the Akhan application
  
  @login @encryption
  Scenario: Login with encrypted credentials
    When I enter username "testuser1"
    And I enter password from encrypted source
    And I click the login button
    Then I should be on the home page
    And I take a screenshot named "login-success"
  
  @navigation @dynamic-xpath
  Scenario: Navigate using dynamic menu XPath
    Given I am logged in
    When I navigate to "ESSS/Series"
    Then I should be on the ESSS page
    When I navigate to "Reference Interests"
    And I navigate to "Interest History"
    And I navigate to "Home"
    Then I should be on the home page
  
  @search @data-driven
  Scenario Outline: ESSS search with custom dropdowns
    Given I am on the ESSS page
    When I select "<SearchType>" from search type dropdown
    And I select "<Attribute>" from attribute dropdown
    And I click search
    Then I should see <ExpectedResult>
    
    Examples:
      | SearchType | Attribute | ExpectedResult |
      | ESSS       | Active    | results        |
      | Series     | Inactive  | results        |
      | ESSS       | Pending   | no results     |
  
  @e2e @comprehensive
  Scenario: Complete workflow
    When I login with username "testuser1" and encrypted password
    And I navigate to "ESSS/Series"
    And I perform search with type "ESSS" and attribute "Active"
    Then I should see search results
    When I navigate through all menu items
    And I logout
    Then I should be on the login page