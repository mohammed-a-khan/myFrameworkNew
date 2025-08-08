@akhan
Feature: Akhan Application Test
  Test login, navigation and ESSS search functionality

  Background:
    Given I am on the login page

  @login
  Scenario: Login to Akhan application
    When I enter username "testuser" and password "testpass"
    And I click the log on button
    Then I should see the home header
    And I should see welcome message for user "testuser"

  @navigation
  Scenario: Verify navigation menu items
    Given I am logged in as "testuser"
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

  @module-navigation
  Scenario Outline: Navigate to different modules
    Given I am logged in as "testuser"
    When I click on "<menuItem>" menu item
    Then I should see the "<pageHeader>" page header

    Examples:
      | menuItem               | pageHeader             |
      | ESSS/Series           | ESSSs/Series          |
      | Reference Interests    | Reference Interests    |
      | Interest History       | Interest History       |
      | External Interests     | External Interests     |
      | System Admin          | System Admin          |
      | Version Information    | Version Information    |

  @file-upload-navigation
  Scenario: Navigate to File Upload module
    Given I am logged in as "testuser"
    When I click on "File Upload" menu item
    Then I should see the "Add files" span element

  @esss-search
  Scenario: Search for ESSS in ESSS/Series module
    Given I am logged in as "testuser"
    When I click on "ESSS/Series" menu item
    Then I should see the "ESSSs/Series" page header
    
    # Verify Type dropdown options
    When I click on the Type dropdown
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
    
    # Select ESSS from Type dropdown
    When I select "ESSS" from Type dropdown
    
    # Verify Attribute dropdown options for ESSS
    When I click on the Attribute dropdown
    Then I should see the following Attribute options:
      | option |
      | Key    |
      | Name   |
      | ID     |
    
    # Select Key from Attribute dropdown
    When I select "Key" from Attribute dropdown
    
    # Enter search value from test data
    When I enter ESSS key from test data
    And I click the search button
    
    # Verify search results
    Then I should see search results in the table
    And I should find ESSS with the entered key in the results