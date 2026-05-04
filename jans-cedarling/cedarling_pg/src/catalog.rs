// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Catalog DDL that ships with the extension.
//!
//! Everything here is wrapped in [`extension_sql!`](pgrx::extension_sql) so `cargo pgrx schema`
//! bundles it into the install script (`cedarling_pg--0.1.0.sql`). The runtime does not create
//! these tables itself — they appear as part of `CREATE EXTENSION cedarling_pg;`.
//!
//! Table design constraints:
//!
//! - Everything lives under the `cedarling` schema to keep the public namespace clean.
//! - `cedarling.mask_rules` is a simple (table, column) registry Keeping the schema now lets us
//!   deliver masking without a follow-up migration.
//! - `cedarling.policy_history` records the result of `cedarling_use_policy` /
//!   `cedarling_rollback_policy` calls. We want a persistent audit trail even across restarts,
//!   so it's a regular table rather than process-local state.
//! - Grants: USAGE on the schema to PUBLIC (so RLS policies invoking our functions work for
//!   any role), but write access to catalog tables is left to the extension owner. Operators
//!   can grant more as needed.

use pgrx::extension_sql;

extension_sql!(
    r"
-- Namespace for cedarling_pg catalog objects.
CREATE SCHEMA IF NOT EXISTS cedarling;
GRANT USAGE ON SCHEMA cedarling TO PUBLIC;

-- Per-(table, column) masking configuration.
-- mask_type is one of: 'null', 'redact', 'partial', 'range', 'hash', 'fixed'.
-- mask_value is mask_type-dependent (Phase 3 uses it for 'partial', 'range', 'fixed').
CREATE TABLE IF NOT EXISTS cedarling.mask_rules (
    table_name   text  NOT NULL,
    column_name  text  NOT NULL,
    mask_type    text  NOT NULL,
    mask_value   text,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now(),
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
",
    name = "cedarling_pg_catalog",
);
