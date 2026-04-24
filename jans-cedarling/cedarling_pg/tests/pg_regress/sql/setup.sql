-- Install / drop / reinstall round-trip. Verifies catalog creation and clean teardown.
CREATE EXTENSION cedarling_pg;

-- Catalog schema should exist after CREATE EXTENSION.
SELECT nspname FROM pg_namespace WHERE nspname = 'cedarling';
SELECT count(*) AS mask_rules_exists
  FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
 WHERE n.nspname = 'cedarling' AND c.relname = 'mask_rules';
SELECT count(*) AS policy_history_exists
  FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
 WHERE n.nspname = 'cedarling' AND c.relname = 'policy_history';

-- Drop + recreate should be idempotent.
DROP EXTENSION cedarling_pg CASCADE;
CREATE EXTENSION cedarling_pg;
