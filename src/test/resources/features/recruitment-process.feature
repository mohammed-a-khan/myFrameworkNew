Feature: Recruitment and Hiring Process
  As a recruitment team
  I want to manage the hiring process
  So that we can efficiently hire qualified candidates

  Background:
    Given I am on the OrangeHRM recruitment module
    And the recruitment workflow is configured

  @smoke @recruitment
  @CSADOTestPlanId("TP-2024-004")
  @CSADOTestSuiteId("TS-RECRUIT-001")
  @CSADOTestCaseId("{TC-REC-001, TC-REC-002}")
  Scenario: Create new job vacancy
    Given I am logged in as HR recruiter "lisa.anderson"
    When I navigate to "Recruitment" > "Vacancies"
    And I click "Add" button
    And I fill vacancy details:
      | Field                  | Value                                        |
      | Job Title             | Senior Software Engineer                     |
      | Vacancy Name          | Sr. Software Engineer - Cloud Platform       |
      | Hiring Manager        | Michael Brown                                |
      | Number of Positions   | 3                                           |
      | Job Description       | src/test/resources/templates/job_desc_sse.txt |
      | Active                | Yes                                         |
    And I define candidate requirements:
      | Requirement Type      | Details                                      |
      | Experience           | 5+ years in software development             |
      | Education            | Bachelor's in Computer Science or equivalent  |
      | Skills               | Java, AWS, Microservices, Docker, Kubernetes |
      | Certifications       | AWS Certified (Preferred)                    |
    And I click "Save" button
    Then the vacancy should be created successfully
    And it should appear in active vacancies list
    And I should be able to publish it to job boards

  @datadriven @bulk-hiring
  @CSDataSource("location=testdata/candidates.xlsx, sheet=BulkHiring, key=CandidateID, filter=Position:SoftwareEngineer;Status:Screened")
  @CSADOTestCaseId("{TC-REC-003}")
  Scenario Outline: Process multiple candidate applications
    Given I have vacancy "Sr. Software Engineer" with ID "VAC-2024-001"
    When I add candidate with details:
      | Field              | Value                    |
      | First Name         | <FirstName>             |
      | Last Name          | <LastName>              |
      | Email             | <Email>                  |
      | Contact Number    | <ContactNumber>          |
      | Resume            | <ResumePath>             |
      | Cover Letter      | <CoverLetterPath>        |
      | LinkedIn Profile  | <LinkedInURL>            |
      | Source            | <Source>                 |
      | Applied Date      | <AppliedDate>            |
    And I perform initial screening
    And I assign screening score "<ScreeningScore>"
    And I add screening notes "<ScreeningNotes>"
    Then the candidate should be moved to "<NextStage>" stage
    And notification should be sent to "<NotifyTo>"

  @api @ats-integration
  @CSADOTestCaseId("{TC-REC-004, TC-REC-005}")
  Scenario: Integrate with external ATS and import candidates
    Given I have configured ATS integration with "Greenhouse"
    When I sync candidates via API:
      """
      {
        "atsProvider": "Greenhouse",
        "syncType": "NEW_APPLICATIONS",
        "dateRange": {
          "from": "2024-01-01",
          "to": "2024-01-31"
        },
        "jobIds": ["VAC-2024-001", "VAC-2024-002"],
        "includeDocuments": true
      }
      """
    Then the API should return candidate data
    And I should successfully import "<CandidateCount>" candidates
    When I view imported candidates in recruitment module
    Then all candidate information should be properly mapped:
      | ATS Field          | OrangeHRM Field         |
      | candidate_name     | Full Name              |
      | email_address      | Email                  |
      | phone_number       | Contact Number         |
      | resume_url         | Resume                 |
      | application_date   | Date of Application    |

  @interview @scheduling
  @CSDataSource("type=database, name=hrmsdb, queryKey=query.recruitment.interview.schedule")
  @CSADOTestCaseId("{TC-REC-006}")
  Scenario: Schedule and conduct interviews
    Given I have shortlisted candidates for "Sr. Software Engineer"
    When I navigate to candidate "John Doe" profile
    And I click "Schedule Interview" button
    And I configure interview details:
      | Field              | Value                    |
      | Interview Type     | Technical Round 1        |
      | Interview Date     | 2024-02-15              |
      | Interview Time     | 10:00 AM                |
      | Duration          | 60 minutes               |
      | Location          | Virtual - Teams Link     |
      | Interviewers      | Tech Lead, Senior Dev    |
    And I add interview guidelines:
      """
      Focus Areas:
      1. System Design - Distributed Systems
      2. Coding - Data Structures & Algorithms
      3. AWS Services and Architecture
      4. Previous Project Experience
      """
    And I send calendar invites
    Then interview should be scheduled successfully
    And calendar invites should be sent to all participants
    And candidate should receive interview details email

  @assessment @technical-evaluation
  @CSADOTestCaseId("{TC-REC-007}")
  Scenario: Conduct technical assessment
    Given candidate "Jane Smith" is scheduled for technical assessment
    When I create an assessment with:
      | Component          | Details                  | Weight |
      | Coding Test       | HackerRank - Medium      | 40%    |
      | System Design     | Design URL Shortener     | 30%    |
      | Technical Q&A     | Java, Spring, AWS        | 20%    |
      | Behavioral        | STAR Questions           | 10%    |
    And the candidate completes the assessment
    And I evaluate the results:
      | Component          | Score | Max Score | Comments                        |
      | Coding Test       | 85    | 100       | Good problem-solving approach   |
      | System Design     | 75    | 100       | Solid design, missed edge cases |
      | Technical Q&A     | 90    | 100       | Strong technical knowledge      |
      | Behavioral        | 80    | 100       | Good communication skills       |
    Then the overall score should be calculated as "82.5%"
    And the candidate should be moved to "Final Interview" stage

  @offer @negotiation
  @CSADOTestCaseId("{TC-REC-008, TC-REC-009}")
  Scenario: Generate and send job offer
    Given candidate "Robert Chen" has cleared all interview rounds
    And I am logged in as HR manager
    When I navigate to "Recruitment" > "Candidates"
    And I select candidate "Robert Chen"
    And I click "Make Offer" button
    And I fill offer details:
      | Field              | Value                    |
      | Position          | Senior Software Engineer |
      | Department        | Cloud Platform Team      |
      | Salary            | $120,000                |
      | Joining Date      | 2024-03-01              |
      | Benefits Package  | Standard Tech Package    |
      | Stock Options     | 1000 RSUs over 4 years  |
      | Signing Bonus     | $10,000                 |
    And I attach offer letter template "tech_offer_template_v2"
    And I set offer expiry date "2024-02-20"
    And I click "Send Offer" button
    Then offer should be generated with all details
    And offer letter should be sent to candidate email
    And offer status should be tracked in system

  @onboarding @hire
  @CSADOTestCaseId("{TC-REC-010}")
  Scenario: Convert candidate to employee
    Given candidate "Emma Thompson" has accepted the offer
    When I navigate to candidate profile
    And I click "Hire" button
    And I initiate onboarding process:
      | Field              | Value                    |
      | Employee ID       | EMP-2024-050            |
      | Start Date        | 2024-03-01              |
      | Reporting Manager | Michael Brown           |
      | Work Location     | New York Office         |
      | Equipment Needed  | Laptop, Monitor, Phone  |
    And I assign onboarding checklist:
      | Task                      | Assigned To        | Due Date    |
      | IT Setup                 | IT Department      | 2024-02-28  |
      | Workspace Allocation     | Facilities         | 2024-02-28  |
      | Benefits Enrollment      | HR Team           | 2024-03-05  |
      | Orientation Session      | HR Team           | 2024-03-01  |
      | Team Introduction        | Hiring Manager     | 2024-03-01  |
    Then employee record should be created
    And onboarding tasks should be assigned
    And welcome email should be sent to new employee

  @reporting @metrics
  @CSDataSource("type=csv, source=testdata/recruitment_metrics.csv, key=MetricID")
  @CSADOTestCaseId("{TC-REC-011}")
  Scenario: Generate recruitment analytics dashboard
    Given I am logged in as recruitment head
    When I navigate to "Recruitment" > "Reports" > "Analytics Dashboard"
    And I select reporting period "Q1 2024"
    Then I should see recruitment metrics:
      | Metric                    | Value    | Target  | Status |
      | Time to Fill             | 35 days  | 30 days | Amber  |
      | Cost per Hire            | $3,500   | $4,000  | Green  |
      | Offer Acceptance Rate    | 85%      | 80%     | Green  |
      | Source Effectiveness     | LinkedIn-40%, Referral-30% | - | - |
      | Interview to Hire Ratio  | 5:1      | 4:1     | Amber  |
    And I should be able to drill down into each metric
    And export dashboard as PDF report