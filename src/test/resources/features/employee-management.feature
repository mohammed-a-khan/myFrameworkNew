Feature: Employee Management System
  As an HR administrator
  I want to manage employee records
  So that I can maintain accurate employee information

  Background:
    Given I am logged in as "Admin" with password "admin123"
    And I navigate to the PIM module

  @smoke @critical @hr
  @CSADOTestPlanId("TP-2024-001")
  @CSADOTestSuiteId("TS-HR-001")
  @CSADOTestCaseId("{TC-EMP-001, TC-EMP-002, TC-EMP-003}")
  Scenario: Add new employee with complete details
    When I click on "Add Employee" button
    And I fill employee details:
      | Field           | Value                    |
      | First Name      | John                     |
      | Middle Name     | Michael                  |
      | Last Name       | Smith                    |
      | Employee Id     | EMP-2024-001            |
      | Photograph      | src/test/resources/images/employee.jpg |
    And I check "Create Login Details" checkbox
    And I enter login credentials:
      | Field           | Value                    |
      | Username        | john.smith.2024          |
      | Password        | ENC(ChF5Mh1NmfP9xYz...)  |
      | Confirm Password| ENC(ChF5Mh1NmfP9xYz...)  |
    And I select "Enabled" for status
    And I click "Save" button
    Then I should see success message "Successfully Saved"
    And I should be redirected to employee details page
    And employee ID "EMP-2024-001" should be displayed
    And I take a screenshot "new_employee_added"

  @regression @negative
  @CSADOTestCaseId("{TC-EMP-004}")
  Scenario: Attempt to add employee with duplicate employee ID
    When I click on "Add Employee" button
    And I fill employee details:
      | Field           | Value                    |
      | First Name      | Jane                     |
      | Last Name       | Doe                      |
      | Employee Id     | EMP-2024-001            |
    And I click "Save" button
    Then I should see error message "Employee Id already exists"
    And the employee should not be saved

  @datadriven @regression
  @CSDataSource("location=testdata/employees.xlsx, sheet=NewEmployees, key=TestCaseID, filter=Department:IT;Status:Active")
  @CSADOTestCaseId("{TC-EMP-005}")
  Scenario: Add multiple employees from Excel data source
    When I click on "Add Employee" button
    And I fill employee details:
      | Field           | Value                    |
      | First Name      | <FirstName>             |
      | Middle Name     | <MiddleName>            |
      | Last Name       | <LastName>              |
      | Employee Id     | <EmployeeID>            |
    And I enter additional information:
      | Field           | Value                    |
      | SSN Number      | <SSN>                   |
      | SIN Number      | <SIN>                   |
      | Date of Birth   | <DOB>                   |
      | Gender          | <Gender>                |
      | Marital Status  | <MaritalStatus>         |
      | Nationality     | <Nationality>           |
    And I click "Save" button
    Then I should see success message "Successfully Saved"
    And employee "<EmployeeID>" should exist in the system

  @api @integration
  @CSADOTestCaseId("{TC-EMP-006, TC-EMP-007}")
  Scenario: Verify employee creation via API and UI synchronization
    When I create an employee via API:
      """
      {
        "firstName": "API",
        "lastName": "TestUser",
        "employeeId": "EMP-API-001",
        "email": "api.testuser@company.com",
        "jobTitle": "QA Engineer",
        "department": "Information Technology",
        "location": "New York",
        "startDate": "2024-01-15"
      }
      """
    Then the API response should have status code 201
    And the response should contain employee ID "EMP-API-001"
    When I search for employee ID "EMP-API-001" in UI
    Then I should find the employee with details:
      | Field           | Expected Value           |
      | Full Name       | API TestUser            |
      | Job Title       | QA Engineer             |
      | Department      | Information Technology  |
      | Location        | New York                |

  @database @validation
  @CSADOTestCaseId("{TC-EMP-008}")
  Scenario: Validate employee data in database after creation
    Given I have created an employee with ID "EMP-DB-001"
    When I query the employee database with:
      """
      SELECT * FROM employees WHERE employee_id = 'EMP-DB-001'
      """
    Then the database should return 1 record
    And the employee record should contain:
      | Column          | Expected Value          |
      | employee_id     | EMP-DB-001             |
      | first_name      | Database               |
      | last_name       | TestUser               |
      | status          | ACTIVE                 |
      | created_date    | {TODAY}                |

  @performance @load
  @CSADOTestCaseId("{TC-EMP-009}")
  Scenario: Bulk employee upload performance test
    Given I have a CSV file "testdata/bulk_employees.csv" with 1000 employee records
    When I navigate to "Admin" > "Data Import"
    And I upload the CSV file
    And I click "Import" button
    Then the import should complete within 60 seconds
    And I should see success message "1000 employees imported successfully"
    And no errors should be reported in the import log

  @security @authentication
  @CSADOTestCaseId("{TC-EMP-010}")
  Scenario: Verify employee login with encrypted credentials
    Given I have an employee with username "secure.user"
    And password stored as "ENC(YmFzZTY0ZW5jb2RlZHBhc3N3b3Jk)"
    When I logout from admin account
    And I login with username "secure.user" and encrypted password
    Then I should be logged in successfully
    And I should see "Employee Self Service" dashboard
    And I should not have access to "Admin" menu