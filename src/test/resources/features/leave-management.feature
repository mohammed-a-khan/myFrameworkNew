Feature: Leave Management System
  As an employee or manager
  I want to manage leave requests
  So that time off can be tracked and approved efficiently

  Background:
    Given I am on the OrangeHRM application
    And the leave management system is configured

  @smoke @leave
  @CSADOTestPlanId("TP-2024-002")
  @CSADOTestSuiteId("TS-LEAVE-001")
  @CSADOTestCaseId("{TC-LEAVE-001, TC-LEAVE-002}")
  Scenario: Employee applies for leave with valid balance
    Given I am logged in as employee "john.smith"
    And I have 10 days of "Annual Leave" balance
    When I navigate to "Leave" > "Apply"
    And I select leave type "Annual Leave"
    And I select from date "2024-12-20"
    And I select to date "2024-12-24"
    And I enter comment "Christmas vacation with family"
    And I click "Apply" button
    Then I should see success message "Successfully Submitted"
    And my leave balance should be reduced to 5 days
    And the leave request should appear in "My Leave" list with status "Pending Approval"

  @datadriven @regression
  @CSDataSource("location=testdata/leave_requests.xlsx, sheet=LeaveApplications, key=TestID, filter=LeaveType:Annual;Environment:QA")
  @CSADOTestCaseId("{TC-LEAVE-003}")
  Scenario Outline: Apply for different types of leave from data source
    Given I am logged in as employee "<EmployeeUsername>"
    And I have <LeaveBalance> days of "<LeaveType>" balance
    When I navigate to "Leave" > "Apply"
    And I select leave type "<LeaveType>"
    And I select from date "<FromDate>"
    And I select to date "<ToDate>"
    And I select partial days option "<PartialDays>"
    And I enter comment "<Comments>"
    And I attach document "<Attachment>" if required
    And I click "Apply" button
    Then I should see message "<ExpectedMessage>"
    And the leave request status should be "<ExpectedStatus>"

  @manager @approval
  @CSADOTestCaseId("{TC-LEAVE-004, TC-LEAVE-005}")
  Scenario: Manager approves team member's leave request
    Given I am logged in as manager "sarah.johnson"
    And I have 3 pending leave requests from my team
    When I navigate to "Leave" > "Leave List"
    And I filter by "Pending Approval" status
    And I select leave request from "John Smith" for dates "2024-12-20 to 2024-12-24"
    And I click "Approve" button
    And I add comment "Approved. Enjoy your vacation!"
    Then the leave status should change to "Approved"
    And an email notification should be sent to "john.smith@company.com"
    And the employee's leave balance should be updated

  @database @validation
  @CSDataSource("type=database, name=hrmsdb, queryKey=query.leave.pending.requests")
  @CSADOTestCaseId("{TC-LEAVE-006}")
  Scenario: Validate leave requests from database
    Given I have leave data from database query
    When I process each pending leave request
    Then the leave request details should match:
      | Database Field   | UI Field              |
      | employee_id      | Employee ID           |
      | leave_type_id    | Leave Type           |
      | date_from        | From Date            |
      | date_to          | To Date              |
      | number_of_days   | Number of Days       |
      | status           | Status               |
    And all pending requests should be displayed in UI

  @api @integration
  @CSADOTestCaseId("{TC-LEAVE-007}")
  Scenario: Create and approve leave via API workflow
    When I create a leave request via API:
      """
      {
        "employeeId": "EMP-2024-100",
        "leaveType": "Sick Leave",
        "fromDate": "2024-11-15",
        "toDate": "2024-11-16",
        "partialDays": "None",
        "comment": "Medical appointment",
        "attachment": null
      }
      """
    Then the API response should have status code 201
    And the response should contain leave request ID
    When I approve the leave request via API:
      """
      {
        "leaveRequestId": "{LEAVE_REQUEST_ID}",
        "action": "APPROVE",
        "comment": "Approved via API"
      }
      """
    Then the API response should have status code 200
    And the leave status in UI should show "Approved"

  @negative @validation
  @CSADOTestCaseId("{TC-LEAVE-008}")
  Scenario: Apply for leave exceeding available balance
    Given I am logged in as employee "test.user"
    And I have 2 days of "Casual Leave" balance
    When I navigate to "Leave" > "Apply"
    And I select leave type "Casual Leave"
    And I select from date "2024-11-20"
    And I select to date "2024-11-25"
    And I click "Apply" button
    Then I should see error message "Leave balance insufficient"
    And the apply button should be disabled
    And I should see available balance as "2.00 Days"

  @reporting @analytics
  @CSADOTestCaseId("{TC-LEAVE-009}")
  Scenario: Generate leave report for department
    Given I am logged in as HR admin
    When I navigate to "Leave" > "Reports" > "Leave Entitlements and Usage Report"
    And I select generate report for:
      | Field            | Value                    |
      | Period          | 2024-01-01 to 2024-12-31|
      | Department      | Information Technology   |
      | Leave Type      | All                      |
      | Include         | Past Employees           |
    And I click "Generate" button
    Then the report should be generated successfully
    And the report should contain:
      | Column                    | Description              |
      | Employee Name            | Full name of employee    |
      | Leave Type               | Type of leave           |
      | Leave Entitlement       | Total days entitled     |
      | Leave Taken             | Days already taken      |
      | Leave Balance           | Remaining balance       |
    And I should be able to export report as "Excel" format

  @csv-driven @bulk
  @CSDataSource("type=csv, source=testdata/bulk_leave_upload.csv, key=RequestID, filter=Status=New;Priority=High")
  @CSADOTestCaseId("{TC-LEAVE-010}")
  Scenario: Bulk upload leave requests from CSV
    Given I am logged in as HR admin
    When I navigate to "Leave" > "Configure" > "Bulk Upload"
    And I upload CSV file with leave requests
    And I click "Validate" button
    Then validation should pass for all records
    When I click "Process" button
    Then all leave requests should be created successfully
    And I should see summary:
      | Metric                   | Count                    |
      | Total Records           | <TotalRecords>           |
      | Successfully Processed  | <SuccessCount>           |
      | Failed Records         | <FailureCount>           |