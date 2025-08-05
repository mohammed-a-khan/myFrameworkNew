-- User management queries

-- QUERY: findUserById
SELECT u.id, u.username, u.email, u.first_name, u.last_name, u.status, u.created_date, u.last_login
FROM users u
WHERE u.id = :userId
AND u.status != 'DELETED';

-- QUERY: findUserByEmail
SELECT u.id, u.username, u.email, u.first_name, u.last_name, u.status, u.created_date, u.last_login
FROM users u
WHERE u.email = :email
AND u.status != 'DELETED';

-- QUERY: findActiveUsers
SELECT u.id, u.username, u.email, u.first_name, u.last_name, u.status, u.created_date, u.last_login
FROM users u
WHERE u.status = 'ACTIVE'
ORDER BY u.last_login DESC
LIMIT ${maxResults};

-- QUERY: updateUserLastLogin
UPDATE users
SET last_login = :loginTime
WHERE id = :userId;

-- QUERY: createUser
INSERT INTO users (username, email, first_name, last_name, password_hash, status, created_date)
VALUES (:username, :email, :firstName, :lastName, :passwordHash, 'ACTIVE', :createdDate);

-- QUERY: deactivateUser
UPDATE users
SET status = 'INACTIVE', modified_date = :modifiedDate
WHERE id = :userId;

-- QUERY: getUserStats
SELECT 
    COUNT(*) as total_users,
    SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) as active_users,
    SUM(CASE WHEN status = 'INACTIVE' THEN 1 ELSE 0 END) as inactive_users,
    SUM(CASE WHEN last_login >= :sinceDate THEN 1 ELSE 0 END) as recent_logins
FROM users
WHERE status != 'DELETED';