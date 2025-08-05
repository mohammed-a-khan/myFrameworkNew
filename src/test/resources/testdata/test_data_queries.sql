-- Query definitions for database-driven test scenarios
-- These queries are referenced in feature files using queryKey parameter

-- query.leave.pending.requests
SELECT 
    lr.id as leave_request_id,
    e.employee_id,
    e.first_name + ' ' + e.last_name as employee_name,
    lt.leave_type_name as leave_type,
    lr.date_from,
    lr.date_to,
    lr.number_of_days,
    lr.comments,
    lr.status
FROM leave_requests lr
JOIN employees e ON lr.employee_id = e.id
JOIN leave_types lt ON lr.leave_type_id = lt.id
WHERE lr.status = 'PENDING_APPROVAL'
  AND lr.date_from >= DATEADD(day, -30, GETDATE())
ORDER BY lr.created_date DESC;

-- query.performance.analytics
SELECT 
    d.department_name,
    COUNT(DISTINCT pr.employee_id) as total_reviews,
    AVG(pr.overall_rating) as avg_rating,
    COUNT(CASE WHEN pr.overall_rating >= 4.0 THEN 1 END) as high_performers,
    COUNT(CASE WHEN pr.overall_rating < 3.0 THEN 1 END) as needs_improvement
FROM performance_reviews pr
JOIN employees e ON pr.employee_id = e.id
JOIN departments d ON e.department_id = d.id
WHERE pr.review_year = YEAR(GETDATE())
  AND pr.status = 'COMPLETED'
GROUP BY d.department_name
ORDER BY avg_rating DESC;

-- query.recruitment.interview.schedule
SELECT 
    c.candidate_id,
    c.first_name + ' ' + c.last_name as candidate_name,
    v.vacancy_name,
    i.interview_type,
    i.interview_date,
    i.interview_time,
    i.location,
    i.status,
    STRING_AGG(int.interviewer_name, ', ') as interviewers
FROM candidates c
JOIN vacancies v ON c.vacancy_id = v.id
JOIN interviews i ON c.candidate_id = i.candidate_id
LEFT JOIN interview_interviewers int ON i.id = int.interview_id
WHERE i.interview_date >= GETDATE()
  AND i.status IN ('SCHEDULED', 'RESCHEDULED')
GROUP BY c.candidate_id, c.first_name, c.last_name, v.vacancy_name,
         i.interview_type, i.interview_date, i.interview_time, i.location, i.status
ORDER BY i.interview_date, i.interview_time;

-- query.test.data.users
SELECT 
    u.username,
    u.encrypted_password,
    u.status,
    r.role_name,
    e.employee_id,
    e.first_name + ' ' + e.last_name as display_name
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
JOIN employees e ON u.employee_id = e.id
WHERE u.status = 'ACTIVE'
  AND u.is_test_user = 1
ORDER BY r.role_name, u.username;