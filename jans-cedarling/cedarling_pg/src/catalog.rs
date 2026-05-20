// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Catalog DDL bundled into `CREATE EXTENSION cedarling_pg;` via [`extension_sql!`](pgrx::extension_sql).
//!
//! All objects live in the `cedarling` schema. `PUBLIC` gets `USAGE`; write access to catalog
//! tables is left to the extension owner.

use pgrx::extension_sql;

// Imports are referenced from the `requires = [...]` list of the second `extension_sql!` block;
// rustc's `unused_imports` check doesn't follow into the macro expansion.
#[allow(unused_imports)]
use crate::policy::versions::{
    cedarling_register_policy_version, cedarling_rollback_policy, cedarling_use_policy,
};

extension_sql!(
    r"
-- Namespace for cedarling_pg catalog objects.
CREATE SCHEMA IF NOT EXISTS cedarling;
GRANT USAGE ON SCHEMA cedarling TO PUBLIC;

-- Per-(table, column) masking configuration.
-- mask_type: 'null' | 'redact' | 'partial' | 'range' | 'hash' | 'fixed'.
-- mask_value: type-specific parameter (pattern, range bounds, fixed string).
-- condition_sql: optional WHERE-clause condition under which the rule applies (NULL = always).
-- data_type: optional explicit type hint for the column (NULL = inferred from context).
CREATE TABLE IF NOT EXISTS cedarling.mask_rules (
    table_name    text  NOT NULL,
    column_name   text  NOT NULL,
    mask_type     text  NOT NULL,
    mask_value    text,
    condition_sql text,
    data_type     text,
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (table_name, column_name),
    CHECK (mask_type IN ('null', 'redact', 'partial', 'range', 'hash', 'fixed'))
);

-- History of policy-set swaps performed via cedarling_use_policy / cedarling_rollback_policy.
-- operation is 'use' or 'rollback'. applied_by is the SQL role that invoked the change.
CREATE TABLE IF NOT EXISTS cedarling.policy_history (
    id             bigserial PRIMARY KEY,
    version        text        NOT NULL,
    previous       text,
    operation      text        NOT NULL,
    applied_by     text        NOT NULL DEFAULT current_user,
    applied_at     timestamptz NOT NULL DEFAULT now(),
    detail         jsonb,
    CHECK (operation IN ('use', 'rollback'))
);

CREATE INDEX IF NOT EXISTS policy_history_applied_at_idx
    ON cedarling.policy_history (applied_at DESC);

COMMENT ON SCHEMA cedarling IS 'cedarling_pg catalog and masking configuration.';
COMMENT ON TABLE  cedarling.mask_rules     IS 'Per-(table,column) masking configuration for cedarling.strategy = mask.';
COMMENT ON TABLE  cedarling.policy_history IS 'Audit trail of policy version swaps performed through cedarling_use_policy / cedarling_rollback_policy.';

-- Explicit mapping from PostgreSQL table OID to Cedar entity type and ID columns.
-- Used by AnyElement-based row helpers.
CREATE TABLE IF NOT EXISTS cedarling.entity_map (
    table_oid    oid PRIMARY KEY,
    entity_type  text NOT NULL,
    id_columns   text[] NOT NULL DEFAULT '{}',
    updated_at   timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE cedarling.entity_map IS 'Optional overrides for table -> Cedar entity mapping used by row authorization.';

-- Named policy-version registry. cedarling_use_policy() resolves a name here before
-- treating the argument as a filesystem path.
CREATE TABLE IF NOT EXISTS cedarling.policy_versions (
    name            text        PRIMARY KEY,
    bootstrap_path  text        NOT NULL,
    registered_at   timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE cedarling.policy_versions IS 'Named policy version registry for cedarling_use_policy / cedarling_register_policy_version.';
",
    name = "cedarling_pg_catalog",
);

extension_sql!(
    r"
REVOKE EXECUTE ON FUNCTION cedarling_use_policy(text) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION cedarling_register_policy_version(text, text) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION cedarling_rollback_policy() FROM PUBLIC;
",
    name = "cedarling_pg_policy_function_privileges",
    requires = [
        cedarling_use_policy,
        cedarling_register_policy_version,
        cedarling_rollback_policy
    ],
);
