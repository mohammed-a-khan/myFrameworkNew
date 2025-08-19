@akhan @framework-demo @azure-devops
Feature: Akhan Application Test
  Demonstrates all CS TestForge Framework features
  Using exact XPaths and requirements
  
  
  Background:
    Given I am on the Akhan application
  
  @login @encryption @TestCaseId:{23232,42323}
  Scenario: Login with encrypted credentials (validates multiple test cases)
    When I enter username "testuser1@americas.cshare.net"
    And I enter password from encrypted source
    And I click the login button
    Then I should be on the home page
    And I take a screenshot named "login-success"
