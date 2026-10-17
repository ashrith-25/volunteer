-- Execute ONLY on an approved development PostgreSQL database. Read-only.
-- Schema and field names may vary by environment; confirm search_path first.
SELECT current_schema(), current_setting('search_path') AS search_path;

SELECT table_schema, table_name, column_name, data_type
FROM information_schema.columns
WHERE table_name IN ('request','volunteers_assigned','users','volunteer_details','user_status')
ORDER BY table_schema, table_name, ordinal_position;

-- Inspect whether the table has a CURRENT indicator or history/status linkage.
SELECT tc.table_schema, tc.table_name, tc.constraint_type,
       kcu.column_name, ccu.table_name AS foreign_table,
       ccu.column_name AS foreign_column
FROM information_schema.table_constraints tc
LEFT JOIN information_schema.key_column_usage kcu
  ON tc.constraint_name = kcu.constraint_name
 AND tc.table_schema = kcu.table_schema
LEFT JOIN information_schema.constraint_column_usage ccu
  ON tc.constraint_name = ccu.constraint_name
 AND tc.table_schema = ccu.table_schema
WHERE tc.table_name = 'volunteers_assigned'
ORDER BY tc.table_schema, tc.constraint_type, kcu.ordinal_position;

-- Verify assignment business rule with service/data owners. Do NOT use MAX(last_update_date)
-- as a surrogate for current assignment without explicit product approval.
