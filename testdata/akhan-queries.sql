-- Akhan Test Queries
-- These queries can be used for database validation in tests

-- Get user permissions
SELECT username, module, access_level 
FROM user_permissions 
WHERE username = ? AND module = ?;

-- Get active ESSS records
SELECT esss_id, esss_name, status, created_date 
FROM esss_records 
WHERE status = 'Active' 
ORDER BY created_date DESC;

-- Get Series by attribute
SELECT series_id, series_name, attribute_type, attribute_value 
FROM series_records 
WHERE attribute_type = ? AND status = ?;

-- Get user activity log
SELECT user_id, action, module, timestamp 
FROM activity_log 
WHERE user_id = ? 
AND timestamp >= ? 
ORDER BY timestamp DESC;

-- Validate reference interests
SELECT ref_id, ref_name, ref_type, status 
FROM reference_interests 
WHERE ref_type = ? AND status = 'Active';

-- Get interest history
SELECT interest_id, user_id, interest_type, action_date 
FROM interest_history 
WHERE user_id = ? 
AND action_date BETWEEN ? AND ?;

-- Check user access rights
SELECT COUNT(*) as has_access 
FROM user_module_access 
WHERE username = ? 
AND module_name = ? 
AND access_level IN ('read', 'write', 'admin');

-- Get file upload history
SELECT file_id, file_name, uploaded_by, upload_date, file_size 
FROM file_uploads 
WHERE uploaded_by = ? 
ORDER BY upload_date DESC 
LIMIT 10;