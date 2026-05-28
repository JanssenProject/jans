// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! SQL predicate helper (`cedarling_where`) — Cedar policy → SQL pushdown.
//!
//! See `cedarling_pg/docs/where-pushdown.md` for end-user semantics and the
//! supported shape matrix. The notes below document constraints on the
//! implementation itself.
//!
//! # Why lexical extraction (and not AST walking)
//!
//! The plan calls for "Use `cedar_policy::Policy::condition()` and walk the
//! AST". The Cedar 4.x **public** API (the `cedar_policy` crate that
//! `cedarling::bindings::cedar_policy` re-exports) does not expose AST
//! inspection on policies — `Policy` has constructors, `to_cedar()`, and
//! scope-constraint accessors, but no `condition()`/expression-walker. The
//! only stable way to introspect a policy's `when` / `unless` body through
//! the public API is its rendered text (`Policy::to_cedar()` /
//! `PolicyMetadata::source`). Pulling in `cedar-policy-core` directly would
//! buy us `ast::Expr`, but that crate is documented as internal and not part
//! of Cedar's stability surface.
//!
//! So this module is deliberately **conservative**: it (a) re-parses every
//! policy via `cedar_policy::Policy::parse` to refuse anything Cedar doesn't
//! consider syntactically valid; (b) does narrow lexical extraction of
//! `when { … }` / `unless { … }` bodies; (c) lowers only a small, named set
//! of structural shapes (`resource.<col> {==,!=,<,<=,>,>=} <literal>`,
//! `resource.<col> in [<literal>, …]`, with `&&` / `||` / `(…)` composition);
//! (d) any policy that doesn't fit those shapes is reported as an
//! **unhandled residual**. The SQL fragment returned for that residual is
//! chosen by `cedarling.where_partial_fallback`: `deny` (default) → `"FALSE"`
//! is safe for standalone callers; `permit` → `"TRUE"` is only safe when
//! paired with a row-by-row `cedarling_authorized*` predicate that re-checks
//! every row. Never invent a SQL predicate the matched Cedar policy didn't
//! actually express.
//!
//! # Linking constraint (read before adding tests)
//!
//! `cargo test --lib` produces a test binary that runs **outside** any Postgres
//! backend. Modern Rust toolchains (>= 1.92) default to `rust-lld` for
//! `x86_64-unknown-linux-gnu`, which refuses to link unresolved Postgres
//! internals such as `SPI_*`, `CurrentMemoryContext`, `PG_exception_stack`,
//! `pfree`, `CopyErrorData`. Functions reachable from a `#[test]` in this
//! module must therefore be **pure Rust** — no `Spi::*`, no `pgrx::log!` /
//! `warning!` macros (which expand to `pg_sys::ereport`). The `#[pg_extern]`
//! entry point and its SPI helpers are exercised exclusively by `#[pg_test]`
//! cases (see `lib.rs::tests::test_cedarling_where_*`), which run **inside**
//! Postgres where those symbols resolve at extension load time.
//!
//! Practically: SQL identifier and literal quoting is done in pure Rust
//! (matching `quote_ident` / `quote_literal` semantics for our restricted
//! inputs) instead of `Spi::get_one_with_args("SELECT quote_ident($1)", …)`.
//! An earlier draft used the SPI-backed builtins; that pulled `SPI_*` symbol
//! references into the unit-test binary via the unit tests reaching
//! `lower_atom` → `quote_ident_sql`, and the test build refused to link.

use cedarling::bindings::cedar_policy;
use cedarling::{PolicyEffect, PolicyMetadata};
use pgrx::prelude::*;
use serde_json::json;

use crate::engine;
use crate::guc_config::{self, CedarlingLogLevelGuc, CedarlingWherePartialFallback};
use crate::observability::log as extension_log;
use crate::resource;
use crate::resource::schema_map::{self, EntityMapping};
use crate::tokens::bundle as token_bundle;

#[derive(Debug, Clone)]
struct TableEntityMapping {
    entity: EntityMapping,
}

/// Lowered representation of one or more Cedar policies translated to a SQL
/// `WHERE`-clause fragment, plus enough metadata for the caller to decide
/// whether row-by-row authorization is still required.
#[derive(Debug, Clone, PartialEq, Eq)]
pub(crate) enum SqlPredicate {
    /// At least one unconditional `permit` matched and no `forbid` blocks it.
    AlwaysTrue,
    /// No `permit` matched and no policy needs row-by-row evaluation.
    AlwaysFalse,
    /// All matched policies were lowered into a SQL fragment.
    Where(String),
    /// At least one matched policy could not be lowered (e.g. function call,
    /// entity-to-entity navigation). The fragment is intentionally permissive
    /// (`TRUE`); RLS on `cedarling_authorized*` enforces those policies.
    Partial {
        fragment: String,
        unhandled_policy_ids: Vec<String>,
    },
}

// ---------------------------------------------------------------------------
// Pure helpers — safe to exercise from `#[cfg(test)] mod tests`.
// ---------------------------------------------------------------------------

/// Pure-Rust equivalent of Postgres `quote_ident`: always wraps in `"…"` and
/// doubles internal `"`. `parse_resource_field` restricts inputs to
/// `[A-Za-z0-9_]+`, so the conservative "always quote" form is equivalent to
/// the Postgres builtin for our use.
pub(crate) fn quote_ident_safe(ident: &str) -> String {
    format!("\"{}\"", ident.replace('"', "\"\""))
}

/// Pure-Rust equivalent of Postgres `quote_literal` for non-binary text:
/// wraps in `'…'` and doubles internal `'`. Assumes
/// `standard_conforming_strings = on` (`PostgreSQL` ≥ 9.1 default), so
/// backslashes are literal and need no special handling.
fn quote_literal_safe(value: &str) -> String {
    format!("'{}'", value.replace('\'', "''"))
}

/// Decode Cedar double-quoted string contents (`\\`, `\"`, `\n`, `\u{…}`, …).
fn unescape_cedar_string_literal(inner: &str) -> Option<String> {
    let mut out = String::with_capacity(inner.len());
    let mut chars = inner.chars();
    while let Some(c) = chars.next() {
        if c != '\\' {
            out.push(c);
            continue;
        }
        let esc = chars.next()?;
        match esc {
            '"' => out.push('"'),
            '\\' => out.push('\\'),
            'n' => out.push('\n'),
            'r' => out.push('\r'),
            't' => out.push('\t'),
            'u' => {
                if chars.next()? != '{' {
                    return None;
                }
                let mut hex = String::new();
                loop {
                    let digit = chars.next()?;
                    if digit == '}' {
                        break;
                    }
                    if !digit.is_ascii_hexdigit() {
                        return None;
                    }
                    hex.push(digit);
                }
                let code = u32::from_str_radix(&hex, 16).ok()?;
                out.push(char::from_u32(code)?);
            },
            _ => return None,
        }
    }
    Some(out)
}

/// Returns true when `bytes[idx]` is a `"` not escaped by an odd-length
/// backslash run directly preceding it.
fn is_unescaped_double_quote(bytes: &[u8], idx: usize) -> bool {
    if bytes.get(idx) != Some(&b'"') {
        return false;
    }
    let mut slash_count = 0usize;
    let mut j = idx;
    while j > 0 {
        if bytes[j - 1] == b'\\' {
            slash_count += 1;
            j -= 1;
        } else {
            break;
        }
    }
    slash_count.is_multiple_of(2)
}

/// Splits `expr` at every top-level occurrence of `op`, ignoring matches
/// inside parentheses or double-quoted string literals.
fn split_top_level(expr: &str, op: &str) -> Vec<String> {
    let mut parts = Vec::new();
    let bytes = expr.as_bytes();
    let op_bytes = op.as_bytes();
    let mut i = 0usize;
    let mut start = 0usize;
    let mut depth = 0i32;
    let mut in_string = false;
    while i < bytes.len() {
        let b = bytes[i];
        if is_unescaped_double_quote(bytes, i) {
            in_string = !in_string;
            i += 1;
            continue;
        }
        if in_string {
            i += 1;
            continue;
        }
        match b {
            b'(' => depth += 1,
            b')' if depth > 0 => depth -= 1,
            _ => {},
        }
        if depth == 0
            && i + op_bytes.len() <= bytes.len()
            && &bytes[i..i + op_bytes.len()] == op_bytes
        {
            parts.push(expr[start..i].trim().to_string());
            i += op_bytes.len();
            start = i;
            continue;
        }
        i += 1;
    }
    parts.push(expr[start..].trim().to_string());
    parts
}

/// Strips paired wrapping `(` / `)` from the outside of `expr` until none
/// remain (preserving inner balanced groups).
fn strip_wrapping_parens(expr: &str) -> &str {
    let mut s = expr.trim();
    loop {
        if !(s.starts_with('(') && s.ends_with(')')) {
            break;
        }
        let mut depth = 0i32;
        let mut in_string = false;
        let mut wraps = true;
        for (idx, ch) in s.char_indices() {
            if ch == '"' && is_unescaped_double_quote(s.as_bytes(), idx) {
                in_string = !in_string;
            }
            if in_string {
                continue;
            }
            if ch == '(' {
                depth += 1;
            } else if ch == ')' {
                depth -= 1;
                if depth == 0 && idx + ch.len_utf8() != s.len() {
                    wraps = false;
                    break;
                }
            }
        }
        if wraps {
            s = s[1..s.len() - 1].trim();
        } else {
            break;
        }
    }
    s
}

/// Recognises `resource.<field>` where `field` is `[A-Za-z0-9_]+`, otherwise
/// returns `None`. Restricting the alphabet means `quote_ident_safe` is
/// equivalent to Postgres `quote_ident` for any value we accept.
fn parse_resource_field(expr: &str) -> Option<String> {
    let expr = expr.trim();
    let rest = expr.strip_prefix("resource.")?;
    let field = rest.trim();
    if field.is_empty() || !field.chars().all(|c| c.is_ascii_alphanumeric() || c == '_') {
        return None;
    }
    Some(field.to_string())
}

/// Lowers a Cedar scalar literal token (`true`, `false`, integers, floats,
/// double-quoted strings) to a SQL literal. Returns `None` for shapes we
/// cannot prove safe to translate.
fn lower_scalar_literal(raw: &str) -> Option<String> {
    let t = raw.trim();
    if t.eq_ignore_ascii_case("true") {
        return Some("TRUE".to_string());
    }
    if t.eq_ignore_ascii_case("false") {
        return Some("FALSE".to_string());
    }
    if t.parse::<i64>().is_ok() || t.parse::<f64>().is_ok() {
        return Some(t.to_string());
    }
    if t.starts_with('"') && t.ends_with('"') && t.len() >= 2 {
        let inner = &t[1..t.len() - 1];
        let decoded = unescape_cedar_string_literal(inner)?;
        return Some(quote_literal_safe(&decoded));
    }
    None
}

/// Lowers a single Cedar comparison atom against `resource.<field>`:
/// `==`, `!=`, `>=`, `<=`, `>`, `<`, `in [..]`. Returns `None` for
/// unsupported shapes (e.g. function calls, principal-side navigation).
fn lower_atom(expr: &str) -> Option<String> {
    let expr = strip_wrapping_parens(expr);
    let in_parts = split_top_level(expr, " in ");
    if in_parts.len() == 2 {
        let field = parse_resource_field(&in_parts[0])?;
        let qfield = quote_ident_safe(&field);
        let rhs = in_parts[1].trim();
        if !(rhs.starts_with('[') && rhs.ends_with(']')) {
            return None;
        }
        let inner = &rhs[1..rhs.len() - 1];
        let values = split_top_level(inner, ",");
        if values.is_empty() {
            return None;
        }
        let mut lowered = Vec::with_capacity(values.len());
        for v in values {
            lowered.push(lower_scalar_literal(&v)?);
        }
        return Some(format!("{qfield} IN ({})", lowered.join(", ")));
    }

    for (cedar_op, sql_op) in [
        ("==", "="),
        ("!=", "<>"),
        (">=", ">="),
        ("<=", "<="),
        (">", ">"),
        ("<", "<"),
    ] {
        let parts = split_top_level(expr, cedar_op);
        if parts.len() == 2 {
            let field = parse_resource_field(&parts[0])?;
            let qfield = quote_ident_safe(&field);
            let rhs = lower_scalar_literal(&parts[1])?;
            return Some(format!("{qfield} {sql_op} {rhs}"));
        }
    }
    None
}

/// Recursively lowers a Cedar boolean condition (`||`, `&&`, atoms).
fn lower_condition(expr: &str) -> Option<String> {
    let expr = strip_wrapping_parens(expr);
    let or_parts = split_top_level(expr, "||");
    if or_parts.len() > 1 {
        let mut lowered = Vec::with_capacity(or_parts.len());
        for p in or_parts {
            lowered.push(format!("({})", lower_condition(&p)?));
        }
        return Some(lowered.join(" OR "));
    }
    let and_parts = split_top_level(expr, "&&");
    if and_parts.len() > 1 {
        let mut lowered = Vec::with_capacity(and_parts.len());
        for p in and_parts {
            lowered.push(format!("({})", lower_condition(&p)?));
        }
        return Some(lowered.join(" AND "));
    }
    lower_atom(expr)
}

/// Lexically extracts all bodies of `when { … }` or `unless { … }` clauses
/// from a Cedar policy source, respecting nested braces and quoted strings.
///
/// Cedar allows stacked `when`/`unless` clauses with implicit AND semantics.
/// Returning all matches preserves that behavior in SQL lowering.
fn extract_clauses(source: &str, clause: &str) -> Option<Vec<String>> {
    let marker = format!("{clause} {{");
    let bytes = source.as_bytes();
    let mut i = 0usize;
    let mut in_string = false;
    let mut top_level_depth = 0i32;
    let mut out = Vec::new();

    while i < bytes.len() {
        let c = bytes[i] as char;
        if c == '"' && is_unescaped_double_quote(bytes, i) {
            in_string = !in_string;
            i += 1;
            continue;
        }
        if in_string {
            i += 1;
            continue;
        }

        if c == '{' {
            top_level_depth += 1;
            i += 1;
            continue;
        }
        if c == '}' {
            if top_level_depth > 0 {
                top_level_depth -= 1;
            }
            i += 1;
            continue;
        }

        if top_level_depth == 0
            && i + marker.len() <= bytes.len()
            && source[i..i + marker.len()] == marker
        {
            let body_start = i + marker.len();
            let mut idx = body_start;
            let mut depth = 1i32;
            let mut body_in_string = false;
            while idx < bytes.len() {
                let ch = bytes[idx] as char;
                if ch == '"' && is_unescaped_double_quote(bytes, idx) {
                    body_in_string = !body_in_string;
                    idx += 1;
                    continue;
                }
                if !body_in_string {
                    if ch == '{' {
                        depth += 1;
                    } else if ch == '}' {
                        depth -= 1;
                        if depth == 0 {
                            let expr = source[body_start..idx].trim().to_string();
                            if expr.is_empty() {
                                return None;
                            }
                            out.push(expr);
                            i = idx + 1;
                            break;
                        }
                    }
                }
                idx += 1;
            }
            if depth != 0 {
                return None;
            }
            continue;
        }

        i += 1;
    }
    Some(out)
}

/// Lowers one `PolicyMetadata` (Cedar source text) to a SQL fragment.
/// Returns `Some("TRUE")` when the policy is unconditional, `None` when the
/// source isn't a Cedar-parseable static policy, or when a `when`/`unless`
/// body uses shapes outside the supported lowering subset (caller treats
/// that as an "unhandled residual" — see `policies_to_sql_predicate`).
///
/// We re-parse via `cedar_policy::Policy::parse` first so a malformed source
/// (which `PolicyMetadata` callers should not produce, but we cannot prove)
/// is treated as unhandled rather than silently mis-lowered.
fn lower_policy_to_sql(meta: &PolicyMetadata) -> Option<String> {
    if cedar_policy::Policy::parse(None, &meta.source).is_err() {
        return None;
    }

    let when_clauses = extract_clauses(&meta.source, "when")?;
    let unless_clauses = extract_clauses(&meta.source, "unless")?;

    if when_clauses.is_empty() && unless_clauses.is_empty() {
        return Some("TRUE".to_string());
    }

    let mut parts: Vec<String> = Vec::new();
    for when_expr in when_clauses {
        parts.push(format!("({})", lower_condition(&when_expr)?));
    }
    for unless_expr in unless_clauses {
        parts.push(format!("NOT ({})", lower_condition(&unless_expr)?));
    }
    if parts.is_empty() {
        Some("TRUE".to_string())
    } else {
        Some(parts.join(" AND "))
    }
}

/// Combines lowered `permit`/`forbid` policies into a single `SqlPredicate`,
/// classifying unconditional cases as `AlwaysTrue` / `AlwaysFalse` and
/// surfacing unhandled policy ids for partial residuals.
fn policies_to_sql_predicate(
    metas: &[PolicyMetadata],
    _table: &TableEntityMapping,
) -> SqlPredicate {
    let mut permit_fragments = Vec::new();
    let mut forbid_fragments = Vec::new();
    let mut unhandled = Vec::new();

    for meta in metas {
        let lowered = lower_policy_to_sql(meta);
        match (meta.effect, lowered) {
            (PolicyEffect::Permit, Some(sql)) => permit_fragments.push(sql),
            (PolicyEffect::Forbid, Some(sql)) => forbid_fragments.push(sql),
            (_, None) => unhandled.push(meta.id.clone()),
        }
    }

    if permit_fragments.is_empty() && unhandled.is_empty() {
        return SqlPredicate::AlwaysFalse;
    }
    if !unhandled.is_empty() {
        return SqlPredicate::Partial {
            fragment: "TRUE".to_string(),
            unhandled_policy_ids: unhandled,
        };
    }

    let permit_sql = if permit_fragments.iter().any(|p| p == "TRUE") {
        "TRUE".to_string()
    } else {
        format!("({})", permit_fragments.join(" OR "))
    };

    if forbid_fragments.is_empty() {
        if permit_sql == "TRUE" {
            SqlPredicate::AlwaysTrue
        } else {
            SqlPredicate::Where(permit_sql)
        }
    } else {
        let forbid_sql = if forbid_fragments.iter().any(|p| p == "TRUE") {
            "TRUE".to_string()
        } else {
            format!("({})", forbid_fragments.join(" OR "))
        };
        if forbid_sql == "TRUE" {
            SqlPredicate::AlwaysFalse
        } else {
            SqlPredicate::Where(format!("({permit_sql}) AND NOT ({forbid_sql})"))
        }
    }
}

fn has_valid_table_and_action_inputs(table_name: &str, action: &str) -> bool {
    !(table_name.trim().is_empty() || action.trim().is_empty())
}

// ---------------------------------------------------------------------------
// SPI / engine helpers — never reach these from `#[test]`. Exercised only by
// `#[pg_extern]` (and the `#[pg_test]` cases that drive it).
// ---------------------------------------------------------------------------

fn resolve_table_oid_via_spi(table_name: &str) -> Option<pg_sys::Oid> {
    // `to_regclass($1)` returns NULL for a missing relation; the bare cast
    // `$1::regclass` would raise `relation "..." does not exist`, which a
    // Postgres longjmp would propagate past `Spi::connect` (the `?`
    // operator can't catch it) and abort the calling SQL statement. Since
    // `cedarling_where` is meant to be embeddable in RLS / view bodies, it
    // must fail-closed silently when the table can't be resolved.
    let mut found: Option<pg_sys::Oid> = None;
    let _ = Spi::connect(|client| {
        let mut rows = client.select(
            "SELECT to_regclass($1)::oid AS oid",
            None,
            &[table_name.into()],
        )?;
        if let Some(row) = rows.next() {
            found = row.get_by_name::<pg_sys::Oid, _>("oid")?;
        }
        Ok::<(), pgrx::spi::Error>(())
    });
    found.filter(|oid| *oid != pg_sys::InvalidOid)
}

fn matching_policies_for_table(
    table: &TableEntityMapping,
    action_trimmed: &str,
    token_src: Option<String>,
) -> Option<Vec<PolicyMetadata>> {
    let engine = engine::global_cedarling().ok()?;
    let resource_json = json!({
        "cedar_entity_mapping": {
            "entity_type": table.entity.entity_type,
            "id": "*",
        }
    });
    let resource_entity = resource::resource_entity_data_from_json_value(resource_json).ok()?;
    let actions = vec![action_trimmed.to_string()];
    let resources = vec![resource_entity];

    if let Some(tokens_json) = token_src {
        let parsed = token_bundle::parse_token_inputs_from_json(&tokens_json).ok()?;
        return engine
            .get_matching_policies_multi_issuer(&parsed, &actions, &resources)
            .ok();
    }

    engine
        .get_matching_policies_unsigned(None, &actions, &resources)
        .ok()
}

// ---------------------------------------------------------------------------
// Public `#[pg_extern]`. Integration coverage lives in `lib.rs` `#[pg_test]`s.
// Do **not** call this from any `#[test]` — see the module-level docs.
// ---------------------------------------------------------------------------

/// Returns a SQL predicate fragment generated from currently matching Cedar
/// policies.
///
/// Token bundle resolution order (first non-empty wins):
///   1. The `tokens` argument (raw JSON string), if non-blank.
///   2. The `cedarling.tokens` GUC, if set and non-blank.
///   3. **Unsigned fallback** — if neither is available, the function uses
///      `Cedarling::get_matching_policies_unsigned(None, …)`. Policies that
///      gate on principal identity (`principal in [...]`) won't match and
///      get filtered out at the policy-store level, so the predicate
///      reflects only rules that hold without an authenticated principal.
///
/// On invalid inputs, table-resolution failures, missing entity-mapping, or
/// engine errors the function returns `"FALSE"` (fail-closed). When at least
/// one matched policy cannot be safely lowered to SQL the function emits a
/// `WARN`-level diagnostic listing the unhandled policy ids and returns a
/// fragment chosen by `cedarling.where_partial_fallback`:
///   - `deny` (default) → `"FALSE"` — safe for standalone use.
///   - `permit` → `"TRUE"` — safe **only** when paired with a row-by-row
///     `cedarling_authorized*` predicate that still enforces every row.
#[pg_extern(stable, parallel_restricted)]
pub fn cedarling_where(table_name: &str, action: &str, tokens: Option<&str>) -> String {
    if !has_valid_table_and_action_inputs(table_name, action) {
        return "FALSE".to_string();
    }
    let action_trimmed = action.trim();
    let table_trimmed = table_name.trim();

    // `token_src = None` is **not** an error here: it triggers the unsigned
    // matching path below. Callers that want a deterministic deny without
    // tokens should rely on the engine being un-bootstrapped, or on the
    // policy store gating every action behind a principal constraint.
    let token_src: Option<String> = tokens
        .map(str::trim)
        .filter(|s| !s.is_empty())
        .map(ToOwned::to_owned)
        .or_else(crate::guc_config::tokens_utf8);

    let Some(table_oid) = resolve_table_oid_via_spi(table_trimmed) else {
        return "FALSE".to_string();
    };
    let Some(entity) = schema_map::mapping_for_table_oid(table_oid).ok().flatten() else {
        return "FALSE".to_string();
    };
    let table = TableEntityMapping { entity };

    let Some(metas) = matching_policies_for_table(&table, action_trimmed, token_src) else {
        return "FALSE".to_string();
    };

    match policies_to_sql_predicate(&metas, &table) {
        SqlPredicate::AlwaysTrue => "TRUE".to_string(),
        SqlPredicate::AlwaysFalse => "FALSE".to_string(),
        SqlPredicate::Where(sql) => sql,
        SqlPredicate::Partial {
            fragment: _,
            unhandled_policy_ids,
        } => {
            let chosen = match guc_config::where_partial_fallback() {
                CedarlingWherePartialFallback::Permit => "TRUE",
                CedarlingWherePartialFallback::Deny => "FALSE",
            };
            extension_log::log_diagnostic(
                CedarlingLogLevelGuc::Warn,
                &format!(
                    "cedarling_where: partial predicate lowering returning '{chosen}'; \
                     unhandled policy ids: {}",
                    unhandled_policy_ids.join(", ")
                ),
            );
            chosen.to_string()
        },
    }
}

// ---------------------------------------------------------------------------
// Unit tests — PURE FUNCTIONS ONLY. See module-level docs for the rationale.
// Do not add any `#[test]` that calls `cedarling_where`,
// `resolve_table_oid_via_spi`, `matching_policies_for_table`, or transitively
// reaches `Spi::*` / `pgrx::log!` / `warning!`. Use `#[pg_test]` (in `lib.rs`)
// for those.
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use std::collections::HashMap;

    use cedarling::PolicyEffect;

    use super::*;

    fn meta(id: &str, effect: PolicyEffect, source: &str) -> PolicyMetadata {
        PolicyMetadata {
            id: id.to_string(),
            effect,
            annotations: HashMap::new(),
            source: source.to_string(),
        }
    }

    fn table_mapping() -> TableEntityMapping {
        TableEntityMapping {
            entity: EntityMapping {
                entity_type: "Test::Row".to_string(),
                id_columns: vec!["id".to_string()],
            },
        }
    }

    #[test]
    fn input_validation_rejects_empty_table_or_action() {
        assert!(
            !has_valid_table_and_action_inputs("", "A::Action::\"Read\""),
            "empty table name should be rejected"
        );
        assert!(
            !has_valid_table_and_action_inputs("t", " "),
            "blank action should be rejected"
        );
        assert!(
            has_valid_table_and_action_inputs("t", "A::Action::\"Read\""),
            "valid table and action should be accepted"
        );
    }

    #[test]
    fn quote_ident_safe_doubles_internal_double_quotes() {
        assert_eq!(
            quote_ident_safe("col"),
            "\"col\"",
            "simple ident should be quoted"
        );
        assert_eq!(
            quote_ident_safe("a\"b"),
            "\"a\"\"b\"",
            "internal double quotes should be doubled"
        );
    }

    #[test]
    fn unescape_cedar_string_literal_decodes_backslash_sequences() {
        assert_eq!(
            unescape_cedar_string_literal(r"C:\\path"),
            Some(r"C:\path".to_string()),
            "Cedar \\\\ should become one backslash"
        );
        assert_eq!(
            unescape_cedar_string_literal(r#"say \"hi\""#),
            Some(r#"say "hi""#.to_string()),
            "Cedar backslash-quote should become a quote"
        );
    }

    #[test]
    fn lower_scalar_literal_unescapes_cedar_strings_before_sql_quoting() {
        assert_eq!(
            lower_scalar_literal(r#""C:\\path""#),
            Some(r"'C:\path'".to_string()),
            "Cedar windows path should lower to matching SQL literal"
        );
    }

    #[test]
    fn quote_literal_safe_doubles_internal_single_quotes() {
        assert_eq!(
            quote_literal_safe("ab"),
            "'ab'",
            "simple literal should be quoted"
        );
        assert_eq!(
            quote_literal_safe("a'b"),
            "'a''b'",
            "internal single quotes should be doubled"
        );
    }

    #[test]
    fn split_top_level_handles_nested_groups_and_quotes() {
        let parts = split_top_level(
            r#"resource.a == "x" && (resource.b == "y" || resource.c == "z")"#,
            "&&",
        );
        assert_eq!(parts.len(), 2, "top-level && should split into two parts");
        // String containing the operator-substring must not be split.
        let parts = split_top_level(r#"resource.a == "&& trick""#, "&&");
        assert_eq!(parts.len(), 1, "&& inside a string must not split");
    }

    #[test]
    fn split_top_level_handles_escaped_backslash_then_quote() {
        let expr = r#"resource.note == "path\\\"segment" && resource.country == "US""#;
        let parts = split_top_level(expr, "&&");
        assert_eq!(
            parts.len(),
            2,
            "escaped backslash before closing quote must not keep parser in string mode"
        );
    }

    #[test]
    fn parse_resource_field_rejects_dotted_or_function_call_lhs() {
        assert_eq!(
            parse_resource_field("resource.foo"),
            Some("foo".into()),
            "simple field"
        );
        assert_eq!(
            parse_resource_field("resource.foo_bar1"),
            Some("foo_bar1".into()),
            "underscore field name"
        );
        assert_eq!(
            parse_resource_field("resource."),
            None,
            "empty field suffix"
        );
        assert_eq!(
            parse_resource_field("resource.foo.bar"),
            None,
            "dotted field path should be rejected"
        );
        assert_eq!(
            parse_resource_field("principal.role"),
            None,
            "non-resource lhs should be rejected"
        );
    }

    #[test]
    fn extract_clauses_reads_when_and_unless_bodies() {
        let src = r#"permit(principal, action, resource) when { resource.a == "x" } unless { resource.b == "y" };"#;
        assert_eq!(
            extract_clauses(src, "when"),
            Some(vec![r#"resource.a == "x""#.to_string()]),
            "when clause body should be extracted"
        );
        assert_eq!(
            extract_clauses(src, "unless"),
            Some(vec![r#"resource.b == "y""#.to_string()]),
            "unless clause body should be extracted"
        );
    }

    #[test]
    fn extract_clauses_collects_stacked_when_and_unless() {
        let src = r#"
            permit(principal, action, resource)
            when { resource.country == "US" }
            when { resource.tier >= 3 }
            unless { resource.blocked == true }
            unless { resource.country == "CN" };
        "#;
        assert_eq!(
            extract_clauses(src, "when"),
            Some(vec![
                r#"resource.country == "US""#.to_string(),
                "resource.tier >= 3".to_string()
            ]),
            "stacked when clauses should all be extracted"
        );
        assert_eq!(
            extract_clauses(src, "unless"),
            Some(vec![
                "resource.blocked == true".to_string(),
                r#"resource.country == "CN""#.to_string()
            ]),
            "stacked unless clauses should all be extracted"
        );
    }

    #[test]
    fn extract_clauses_handles_backslash_quote_sequences_in_strings() {
        let src = r#"
            permit(principal, action, resource)
            when { resource.note == "path\\\"segment}" && resource.country == "US" }
            when { resource.tier >= 3 };
        "#;
        let when = extract_clauses(src, "when").expect("clauses must parse");
        assert_eq!(when.len(), 2, "both when clauses must be extracted");
        assert!(
            when[0].contains(r#"resource.country == "US""#),
            "first clause must remain intact after escaped quote processing"
        );
    }

    #[test]
    fn lower_condition_handles_eq_lt_gt_in_and_boolean_chains() {
        let expr = r#"(resource.country == "US" && resource.age >= 18) || resource.tier in ["gold","platinum"]"#;
        let lowered = lower_condition(expr).expect("lowering should succeed");
        assert!(
            lowered.contains("\"country\" = 'US'"),
            "country equality should lower"
        );
        assert!(
            lowered.contains("\"age\" >= 18"),
            "age comparison should lower"
        );
        assert!(
            lowered.contains("\"tier\" IN ('gold', 'platinum')"),
            "IN list should lower"
        );
        assert!(lowered.contains(" OR "), "OR chain should be preserved");
        assert!(lowered.contains(" AND "), "AND chain should be preserved");
    }

    #[test]
    fn lower_condition_rejects_unsupported_shapes() {
        // Function call: not lowerable.
        assert_eq!(
            lower_condition(r#"principal.role.contains("admin")"#),
            None,
            "function-call lhs should not lower"
        );
        assert_eq!(
            lower_condition(r#"resource.owner.id == "alice""#),
            None,
            "multi-segment field access should not lower"
        );
    }

    #[test]
    fn lower_policy_to_sql_unconditional_returns_true() {
        let m = meta(
            "p1",
            PolicyEffect::Permit,
            "permit(principal, action, resource);",
        );
        assert_eq!(
            lower_policy_to_sql(&m).as_deref(),
            Some("TRUE"),
            "unconditional permit should lower to TRUE"
        );
    }

    #[test]
    fn lower_policy_to_sql_combines_stacked_when_clauses_with_and() {
        let m = meta(
            "p1",
            PolicyEffect::Permit,
            r#"permit(principal, action, resource)
               when { resource.country == "US" }
               when { resource.tier >= 3 };"#,
        );
        let sql = lower_policy_to_sql(&m).expect("stacked when should lower");
        assert!(
            sql.contains("\"country\" = 'US'"),
            "first when clause should appear"
        );
        assert!(
            sql.contains("\"tier\" >= 3"),
            "second when clause should appear"
        );
        assert!(
            sql.contains(" AND "),
            "stacked when clauses should AND together"
        );
    }

    #[test]
    fn lower_policy_to_sql_rejects_unparseable_source() {
        // Source that doesn't survive `cedar_policy::Policy::parse` is treated
        // as unhandled (None), not silently lowered. PolicyMetadata callers
        // shouldn't produce malformed text, but we don't trust the input.
        let m = meta(
            "p1",
            PolicyEffect::Permit,
            "this is not a Cedar policy at all",
        );
        assert_eq!(
            lower_policy_to_sql(&m),
            None,
            "unparseable policy source should not lower"
        );
    }

    #[test]
    fn policies_to_sql_predicate_yields_partial_for_unhandled_policy() {
        let policies = vec![
            meta(
                "p1",
                PolicyEffect::Permit,
                r"permit(principal, action, resource) when { resource.age >= 18 };",
            ),
            meta(
                "p2",
                PolicyEffect::Permit,
                r#"permit(principal, action, resource) when { principal.role.contains("admin") };"#,
            ),
        ];
        match policies_to_sql_predicate(&policies, &table_mapping()) {
            SqlPredicate::Partial {
                fragment,
                unhandled_policy_ids,
            } => {
                assert_eq!(fragment, "TRUE", "handled permit should contribute TRUE");
                assert_eq!(
                    unhandled_policy_ids,
                    vec!["p2".to_string()],
                    "unhandled policy id should be reported"
                );
            },
            other => panic!("expected Partial with fragment TRUE and unhandled p2, got {other:?}"),
        }
    }

    #[test]
    fn policies_to_sql_predicate_yields_always_false_when_no_permit_matches() {
        let policies: Vec<PolicyMetadata> = vec![];
        assert_eq!(
            policies_to_sql_predicate(&policies, &table_mapping()),
            SqlPredicate::AlwaysFalse,
            "empty policy set should yield AlwaysFalse"
        );
    }

    #[test]
    fn policies_to_sql_predicate_honors_forbid_overrides() {
        let policies = vec![
            meta(
                "allow",
                PolicyEffect::Permit,
                r"permit(principal, action, resource) when { resource.age >= 18 };",
            ),
            meta(
                "deny",
                PolicyEffect::Forbid,
                r#"forbid(principal, action, resource) when { resource.country == "CN" };"#,
            ),
        ];
        match policies_to_sql_predicate(&policies, &table_mapping()) {
            SqlPredicate::Where(sql) => {
                assert!(sql.contains("\"age\" >= 18"), "permit clause should appear");
                assert!(
                    sql.contains("\"country\" = 'CN'"),
                    "forbid clause should appear"
                );
                assert!(sql.contains("NOT"), "forbid should wrap clause in NOT");
            },
            other => panic!("expected WHERE predicate combining permit and forbid, got {other:?}"),
        }
    }
}
