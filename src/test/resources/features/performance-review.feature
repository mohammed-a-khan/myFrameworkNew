Feature: Performance Review Management
  As an organization
  I want to manage employee performance reviews
  So that employee growth and achievements can be tracked

  Background:
    Given the performance review cycle is active
    And I am using the OrangeHRM application

  @smoke @performance
  @CSADOTestPlanId("TP-2024-003")
  @CSADOTestSuiteId("TS-PERF-001")
  @CSADOTestCaseId("{TC-PERF-001, TC-PERF-002, TC-PERF-003}")
  Scenario: Manager initiates performance review for team member
    Given I am logged in as manager "michael.brown"
    And I have 5 direct reports
    When I navigate to "Performance" > "Manage Reviews" > "Manage Performance Reviews"
    And I click "Add" button
    And I fill review details:
      | Field              | Value                    |
      | Employee Name      | John Smith              |
      | Supervisor         | Michael Brown           |
      | Review Period From | 2024-01-01              |
      | Review Period To   | 2024-12-31              |
      | Due Date          | 2024-12-15              |
    And I add reviewers:
      | Reviewer Type      | Reviewer Name           |
      | Supervisor         | Michael Brown           |
      | Peer              | Jane Doe                |
      | Peer              | Robert Johnson          |
      | Self              | John Smith              |
    And I click "Activate" button
    Then the review should be created with status "In Progress"
    And email notifications should be sent to all reviewers
    And the review should appear in "Active Reviews" list

  @datadriven @excel
  @CSDataSource("location=testdata/performance_reviews.xlsx, sheet=ReviewData, key=ReviewID, filter=Department:Engineering;Year:2024")
  @CSADOTestCaseId("{TC-PERF-004}")
  Scenario Outline: Create multiple performance reviews from Excel
    Given I am logged in as HR admin
    When I navigate to "Performance" > "Manage Reviews"
    And I create a performance review with data:
      | Field              | Value                    |
      | Employee          | <EmployeeName>           |
      | Job Title         | <JobTitle>               |
      | Department        | <Department>             |
      | Review Type       | <ReviewType>             |
      | Review Period     | <ReviewPeriod>           |
      | KPIs              | <KPIs>                   |
      | Competencies      | <Competencies>           |
    And I assign reviewers from "<ReviewersList>"
    And I set due date as "<DueDate>"
    And I activate the review
    Then the review should be created successfully
    And "<NotificationCount>" notifications should be sent

  @api @integration
  @CSADOTestCaseId("{TC-PERF-005, TC-PERF-006}")
  Scenario: Complete performance review via API and verify in UI
    Given I have an active review with ID "REV-2024-001"
    When I submit review feedback via API:
      """
      {
        "reviewId": "REV-2024-001",
        "reviewerId": "EMP-100",
        "reviewerType": "SUPERVISOR",
        "ratings": {
          "goalAchievement": 4.5,
          "communication": 4.0,
          "teamwork": 4.5,
          "leadership": 4.0,
          "innovation": 3.5
        },
        "comments": {
          "strengths": "Excellent technical skills and problem-solving ability",
          "improvements": "Could improve on delegation and time management",
          "overallComments": "Strong performer with good potential for growth"
        },
        "recommendedActions": ["Promotion", "Salary Increase", "Leadership Training"]
      }
      """
    Then the API response should have status code 200
    When I view the review "REV-2024-001" in UI
    Then I should see the supervisor feedback is submitted
    And the overall rating should be "4.2"
    And recommended actions should include "Promotion"

  @database @reporting
  @CSDataSource("type=database, name=hrmsdb, queryKey=query.performance.analytics")
  @CSADOTestCaseId("{TC-PERF-007}")
  Scenario: Generate performance analytics from database
    Given I am logged in as HR director
    When I navigate to "Performance" > "Reports" > "Performance Analytics"
    And I configure report parameters:
      | Parameter          | Value                    |
      | Report Type       | Department Performance   |
      | Period           | Last 12 Months          |
      | Departments      | All                     |
      | Include          | Completed Reviews Only  |
    And I click "Generate Analytics" button
    Then the report should display:
      | Metric                    | Data Type               |
      | Average Rating by Dept    | Bar Chart              |
      | Rating Distribution       | Pie Chart              |
      | Year-over-Year Trend     | Line Graph             |
      | Top Performers           | Data Table             |
      | Improvement Areas        | Word Cloud             |
    And I should be able to drill down into department details

  @goals @objectives
  @CSADOTestCaseId("{TC-PERF-008}")
  Scenario: Set and track employee goals
    Given I am logged in as employee "emma.wilson"
    When I navigate to "Performance" > "My Goals"
    And I click "Add Goal" button
    And I create a SMART goal:
      | Field              | Value                                           |
      | Goal Title        | Complete AWS Certification                      |
      | Category          | Professional Development                        |
      | Description       | Obtain AWS Solutions Architect certification    |
      | Target Date       | 2024-06-30                                     |
      | Success Metrics   | Pass certification exam with 80% or higher      |
      | Weight           | 20%                                            |
    And I click "Save" button
    Then the goal should be created with status "Active"
    When I update goal progress to "75%" with note "Completed training, exam scheduled"
    Then the goal progress should be reflected in my performance dashboard

  @360feedback @multi-rater
  @CSDataSource("location=testdata/360_feedback.json, key=FeedbackID, filter=Status:Pending")
  @CSADOTestCaseId("{TC-PERF-009}")
  Scenario: Complete 360-degree feedback process
    Given I am participating in 360 feedback for "Sarah Chen"
    And I am assigned as a "Peer" reviewer
    When I access the feedback form via email link
    And I provide ratings for competencies:
      | Competency              | Rating | Comments                           |
      | Communication          | 4      | Clear and concise communicator     |
      | Collaboration          | 5      | Excellent team player              |
      | Problem Solving        | 4      | Creative solutions to challenges   |
      | Technical Skills       | 5      | Deep expertise in domain           |
      | Leadership Potential   | 3      | Shows promise, needs experience    |
    And I provide overall feedback:
      """
      Sarah is a valuable team member who consistently delivers high-quality work.
      Her technical skills are exceptional, and she's always willing to help others.
      With more leadership opportunities, she could excel in a senior role.
      """
    And I submit the feedback
    Then I should see confirmation "Feedback submitted successfully"
    And the feedback should be recorded anonymously
    And completion rate should update in the system

  @calibration @review-cycle
  @CSADOTestCaseId("{TC-PERF-010, TC-PERF-011}")
  Scenario: Performance calibration session
    Given I am logged in as HR business partner
    And calibration session "Q4-2024-Engineering" is scheduled
    When I navigate to "Performance" > "Calibration Sessions"
    And I join the active session
    Then I should see all employees under review:
      | Employee Name | Manager Rating | Self Rating | Peer Average | Status    |
      | John Smith   | 4.2           | 4.5         | 4.1          | Pending   |
      | Jane Doe     | 3.8           | 4.0         | 3.9          | Pending   |
      | Bob Wilson   | 4.5           | 4.3         | 4.4          | Pending   |
    When managers discuss and calibrate ratings
    And I update calibrated ratings:
      | Employee Name | Original | Calibrated | Justification                |
      | John Smith   | 4.2      | 4.0        | Aligned with team average    |
      | Jane Doe     | 3.8      | 3.8        | No change required          |
      | Bob Wilson   | 4.5      | 4.5        | Exceptional performer       |
    And I click "Finalize Calibration" button
    Then all ratings should be updated in the system
    And calibration report should be generated