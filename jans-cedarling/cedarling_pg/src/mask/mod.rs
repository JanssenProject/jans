// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! SQL masking helpers backed by `cedarling.mask_rules` + the built-in default registry.

pub(crate) mod config;
pub(crate) mod types;

use std::collections::BTreeMap;
use std::sync::Mutex;
#[cfg(not(test))]
use std::sync::atomic::AtomicBool;

use pgrx::prelude::*;
use serde_json::{json, Value};

use crate::guc_config;
use crate::mask::config::{default_mask_for_column, lookup_explicit_rules, MaskRule};
use crate::mask::types::MaskType;

static LAST_MASKED_ROW: Mutex<Option<Value>> = Mutex::new(None);
#[cfg(not(test))]
static HASH_SALT_MISSING_WARNED: AtomicBool = AtomicBool::new(false);

#[cfg(not(test))]
fn warn_hash_salt_missing(col: &str, table_name: &str) {
    if HASH_SALT_MISSING_WARNED.swap(true, std::sync::atomic::Ordering::Relaxed) {
        return;
    }
    warning!(
        "cedarling_pg: cedarling.mask_hash_salt is not set; hash masking for column \
         '{col}' in table '{table_name}' requires a salt"
    );
}

#[cfg(test)]
fn warn_hash_salt_missing(_col: &str, _table_name: &str) {
    // Unit tests run outside Postgres; keep this path side-effect free.
}

/// Upsert a masking rule in `cedarling.mask_rules`.
#[pg_extern]
pub fn cedarling_set_mask_config(
    table_name: &str,
    column_name: &str,
    mask_type: &str,
    mask_value: Option<&str>,
) -> bool {
    if table_name.trim().is_empty() || column_name.trim().is_empty() || mask_type.trim().is_empty()
    {
        return false;
    }
    Spi::connect_mut(|client| {
        client.update(
            "INSERT INTO cedarling.mask_rules(table_name,column_name,mask_type,mask_value,updated_at)
             VALUES ($1,$2,$3,$4,now())
             ON CONFLICT (table_name,column_name)
             DO UPDATE SET mask_type = EXCLUDED.mask_type, mask_value = EXCLUDED.mask_value, updated_at = now()",
            None,
            &[table_name.into(), column_name.into(), mask_type.into(), mask_value.into()],
        )?;
        Ok::<(), pgrx::spi::Error>(())
    })
    .is_ok()
}

/// Preview masking logic as a pure SQL function — useful for interactive testing.
#[pg_extern]
pub fn cedarling_test_masking(
    original_value: Option<&str>,
    _data_type: Option<&str>,
    mask_type: &str,
    mask_config: Option<&str>,
) -> Option<String> {
    let salt = guc_config::mask_hash_salt_bytes();
    let mt = MaskType::from_parts(mask_type, mask_config);
    let result = mt.apply(original_value, &salt);
    if matches!(mt, MaskType::Hash) && result.as_deref() == Some("[HASH_SALT_REQUIRED]") {
        warning!("cedarling_pg: cedarling.mask_hash_salt is not set; hash masking requires a salt");
    }
    result
}

/// Return masking rules for a table/action as a JSONB document.
///
/// Each rule entry includes `column`, `mask_type`, `mask_value`, `condition_sql`, and `data_type`.
#[pg_extern]
pub fn cedarling_mask_plan(table_name: &str, action: Option<&str>) -> pgrx::datum::JsonB {
    let action_value = action.unwrap_or("Read");
    let mut rules: Vec<Value> = Vec::new();
    let _ = Spi::connect(|client| {
        let rows = client.select(
            "SELECT column_name, mask_type, mask_value, condition_sql, data_type
             FROM cedarling.mask_rules
             WHERE table_name = $1
             ORDER BY column_name",
            None,
            &[table_name.into()],
        )?;
        for row in rows {
            let col = row
                .get_by_name::<String, _>("column_name")?
                .unwrap_or_default();
            let mt = row
                .get_by_name::<String, _>("mask_type")?
                .unwrap_or_default();
            let mv = row.get_by_name::<String, _>("mask_value")?;
            let cs = row.get_by_name::<String, _>("condition_sql")?;
            let dt = row.get_by_name::<String, _>("data_type")?;
            rules.push(json!({
                "column":        col,
                "mask_type":     mt,
                "mask_value":    mv,
                "condition_sql": cs,
                "data_type":     dt,
            }));
        }
        Ok::<(), pgrx::spi::Error>(())
    });
    pgrx::datum::JsonB(json!({
        "table_name": table_name,
        "action":     action_value,
        "rules":      rules,
    }))
}

/// Apply configured masking rules to a JSONB row, stash the result for [`cedarling_get_masked_row`],
/// and return the masked JSONB.
///
/// Explicit rules in `cedarling.mask_rules` take precedence; the built-in default registry is
/// applied to columns that have no explicit rule but whose name matches a known sensitive pattern.
/// Type-preserving: `Range`-masked columns are written as JSON numbers.
#[pg_extern]
#[allow(clippy::needless_pass_by_value)] // `#[pg_extern]` maps Rust parameters from PostgreSQL call convention; JsonB is moved in.
pub fn cedarling_mask_row(
    row_json: pgrx::datum::JsonB,
    table_name: &str,
    _action: Option<&str>,
) -> pgrx::datum::JsonB {
    let salt = guc_config::mask_hash_salt_bytes();
    let masked = compute_masked_row_inner(&row_json.0, table_name, &salt);
    if let Ok(mut slot) = LAST_MASKED_ROW.lock() {
        *slot = Some(masked.clone());
    }
    pgrx::datum::JsonB(masked)
}

/// Return the last masked row produced by this backend process (or `NULL` if none).
#[pg_extern]
pub fn cedarling_get_masked_row() -> Option<pgrx::datum::JsonB> {
    LAST_MASKED_ROW
        .lock()
        .ok()
        .and_then(|g| g.clone().map(pgrx::datum::JsonB))
}

/// Compute the masked version of `row`, stash it in [`LAST_MASKED_ROW`], and return it.
///
/// Called from `cedarling_authorized_row` when `cedarling.strategy = mask` and the Cedar
/// engine returned a deny decision.
pub(crate) fn compute_and_stash_masked_row(row: &Value, table_name: &str, salt: &[u8]) -> Value {
    let masked = compute_masked_row_inner(row, table_name, salt);
    if let Ok(mut slot) = LAST_MASKED_ROW.lock() {
        *slot = Some(masked.clone());
    }
    masked
}

/// Core masking logic: explicit catalog rules + default registry fallback, type-preserving output.
fn compute_masked_row_inner(row: &Value, table_name: &str, salt: &[u8]) -> Value {
    let mut out = row.clone();
    let explicit: BTreeMap<String, MaskRule> = lookup_explicit_rules(table_name)
        .into_iter()
        .map(|r| (r.column_name.clone(), r))
        .collect();

    if let Some(obj) = out.as_object_mut() {
        let column_names: Vec<String> = obj.keys().cloned().collect();
        for col in &column_names {
            // Explicit rule wins when applicable; otherwise fall back to default registry.
            let mask_opt = explicit.get(col).and_then(resolve_explicit_mask).or_else(|| {
                default_mask_for_column(col)
            });
            let Some(mask) = mask_opt else { continue };

            let original = match obj.get(col) {
                Some(Value::String(s)) => Some(s.as_str()),
                Some(Value::Null) | None => None,
                Some(other) => {
                    // Stringify numbers / booleans so they can be passed to apply()
                    let s = other.to_string();
                    // We can't borrow `s` after this point, so clone into a temp owned value.
                    let tmp = s.clone();
                    let masked_val = apply_with_type_preservation(&mask, Some(tmp.as_str()), salt);
                    obj.insert(col.clone(), masked_val);
                    continue;
                },
            };

            if matches!(mask, MaskType::Hash)
                && mask.apply(original, salt).as_deref() == Some("[HASH_SALT_REQUIRED]")
            {
                warn_hash_salt_missing(col, table_name);
            }

            let masked_val = apply_with_type_preservation(&mask, original, salt);
            obj.insert(col.clone(), masked_val);
        }
    }
    out
}

/// Apply `mask` and convert the result to the appropriate JSON value type.
///
/// `Range` returns a JSON `Number`; all other variants return `String` or `Null`.
fn apply_with_type_preservation(mask: &MaskType, original: Option<&str>, salt: &[u8]) -> Value {
    match mask.apply(original, salt) {
        None => Value::Null,
        Some(s) if mask.preserves_numeric() => {
            if let Ok(n) = s.parse::<i64>() {
                Value::Number(n.into())
            } else if let Ok(f) = s.parse::<f64>() {
                serde_json::Number::from_f64(f).map_or(Value::String(s), Value::Number)
            } else {
                Value::String(s)
            }
        },
        Some(s) => Value::String(s),
    }
}

fn resolve_explicit_mask(rule: &MaskRule) -> Option<MaskType> {
    if let Some(condition_sql) = rule.condition_sql.as_deref() {
        if condition_sql.trim().is_empty() {
            return Some(rule.mask_type.clone());
        }
        warning!(
            "cedarling_pg: ignoring condition_sql for column '{}' to avoid executing arbitrary SQL",
            rule.column_name
        );
        return None;
    }
    Some(rule.mask_type.clone())
}

#[cfg(test)]
mod tests {
    use serde_json::json;

    use super::*;

    const SALT: &[u8] = b"cedarling-pg-test-salt";

    fn masked_row(row: &Value, table: &str) -> Value {
        compute_masked_row_inner(row, table, SALT)
    }

    #[test]
    fn redact_mask_applied_to_explicit_rule() {
        // We can't call SPI in unit tests, so test the inner logic by passing explicit rules
        // directly through types.
        let mt = MaskType::Redact;
        let result = apply_with_type_preservation(&mt, Some("alice@example.com"), SALT);
        assert_eq!(result, Value::String("***REDACTED***".to_string()));
    }

    #[test]
    fn range_mask_produces_json_number() {
        let mt = MaskType::Range {
            min: 50_000,
            max: 150_000,
        };
        let result = apply_with_type_preservation(&mt, Some("alice"), SALT);
        assert!(
            result.is_number(),
            "Range should produce JSON Number, got: {result}"
        );
        let n = result.as_i64().expect("should be integer");
        assert!((50_000..=150_000).contains(&n));
    }

    #[test]
    fn null_mask_produces_json_null() {
        let mt = MaskType::Null;
        let result = apply_with_type_preservation(&mt, Some("something"), SALT);
        assert!(result.is_null());
    }

    #[test]
    fn compute_masked_row_applies_default_registry_to_email_column() {
        // In unit tests, lookup_explicit_rules returns empty (no SPI).
        let row = json!({"user_email": "alice@example.org", "name": "alice"});
        let result = masked_row(&row, "test_tbl_no_explicit_rules");
        // user_email matches default registry → should be masked
        let email_val = result.get("user_email").unwrap();
        assert_ne!(
            email_val.as_str(),
            Some("alice@example.org"),
            "email should be masked by default registry"
        );
        // name has no default match → preserved
        assert_eq!(result.get("name").and_then(|v| v.as_str()), Some("alice"));
    }

    #[test]
    fn compute_masked_row_masks_password_column() {
        let row = json!({"password": "s3cr3t", "role": "user"});
        let result = masked_row(&row, "test_tbl");
        assert_eq!(
            result.get("password").and_then(|v| v.as_str()),
            Some("[PROTECTED]"),
            "password should be masked to [PROTECTED]"
        );
        assert_eq!(result.get("role").and_then(|v| v.as_str()), Some("user"));
    }
}
