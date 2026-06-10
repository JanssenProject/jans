// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Catalog DDL bundled into `CREATE EXTENSION cedarling_pg;` via [`extension_sql!`](pgrx::extension_sql).
//!
//! All objects live in the `cedarling` schema. `PUBLIC` gets `USAGE`; write access to catalog
//! tables is left to the extension owner.

use pgrx::extension_sql;

// The `requires = [...]` list on the second `extension_sql!` block below is
// resolved by pgrx's SQL entity graph by *bare entity name* — fully qualified
// paths there fail with "could not find `requires` target". Bringing the bare
// names into scope keeps the macro syntactically valid; `#[allow(unused_imports)]`
// is required because the macro expands to graph metadata, not a Rust reference,
// so rustc's lint pass cannot see these imports as used.
#[allow(unused_imports)]
use crate::mask::cedarling_set_mask_config;
#[allow(unused_imports)]
use crate::observability::status::cedarling_status;
#[allow(unused_imports)]
use crate::observability::trace::{
    cedarling_explain, cedarling_last_trace, cedarling_recent_traces,
};
#[allow(unused_imports)]
use crate::policy::versions::{
    cedarling_diff_policies, cedarling_register_policy_version, cedarling_rollback_policy,
    cedarling_use_policy,
};
#[allow(unused_imports)]
use crate::resource::schema_map::cedarling_register_entity_map;

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
-- Policy-lifecycle functions: file-system access + global policy swap.
REVOKE EXECUTE ON FUNCTION cedarling_use_policy(text) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION cedarling_register_policy_version(text, text) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION cedarling_rollback_policy() FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION cedarling_diff_policies(text, text) FROM PUBLIC;

-- Authorization-input mutators: cedarling.mask_rules controls what columns get
-- masked; cedarling.entity_map controls which Cedar entity a row evaluates as.
-- Both are decision-shaping inputs, so the writers are owner-only.
REVOKE EXECUTE ON FUNCTION cedarling_set_mask_config(text, text, text, text) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION cedarling_register_entity_map(oid, text, text[]) FROM PUBLIC;

-- Defense in depth: PostgreSQL grants no PUBLIC privileges on these tables by
-- default, but make it explicit so a later `GRANT ALL ... TO PUBLIC` in a
-- different migration doesn't silently open the door.
REVOKE INSERT, UPDATE, DELETE, TRUNCATE ON cedarling.mask_rules FROM PUBLIC;
REVOKE INSERT, UPDATE, DELETE, TRUNCATE ON cedarling.entity_map FROM PUBLIC;

-- Observability readers: the trace ring is per-backend, so under connection
-- pooling (PgBouncer / app-side pools) a low-privilege role can read trace
-- entries produced by other sessions that previously ran on the same backend.
-- Entries surface resource_type / resource_id / principal_id / diag_errors —
-- authorization metadata, not row contents, but still cross-session leakage.
-- Lock these to the extension owner; grant to a dedicated observability role
-- as needed.
REVOKE EXECUTE ON FUNCTION cedarling_last_trace() FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION cedarling_recent_traces(int) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION cedarling_explain(text, text) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION cedarling_status() FROM PUBLIC;
",
    name = "cedarling_pg_policy_function_privileges",
    requires = [
        cedarling_use_policy,
        cedarling_register_policy_version,
        cedarling_rollback_policy,
        cedarling_diff_policies,
        cedarling_set_mask_config,
        cedarling_register_entity_map,
        cedarling_last_trace,
        cedarling_recent_traces,
        cedarling_explain,
        cedarling_status,
    ],
);
