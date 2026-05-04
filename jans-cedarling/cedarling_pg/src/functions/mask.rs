// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! SQL masking helpers backed by `cedarling.mask_rules`.

use std::collections::BTreeMap;
use std::hash::{DefaultHasher, Hash, Hasher};
use std::sync::Mutex;

use pgrx::prelude::*;
use serde_json::{json, Value};

static LAST_MASKED_ROW: Mutex<Option<Value>> = Mutex::new(None);

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

/// Preview masking logic as a pure SQL function.
#[pg_extern]
pub fn cedarling_test_masking(
    original_value: Option<&str>,
    _data_type: Option<&str>,
    mask_type: &str,
    mask_config: Option<&str>,
) -> Option<String> {
    apply_mask(original_value, mask_type, mask_config)
}

/// Return masking rules for a table/action as JSON.
#[pg_extern]
pub fn cedarling_mask_plan(table_name: &str, action: Option<&str>) -> pgrx::datum::JsonB {
    let action_value = action.unwrap_or("Read");
    let mut rules: Vec<Value> = Vec::new();
    let _ = Spi::connect(|client| {
        let q = "SELECT column_name, mask_type, mask_value
                 FROM cedarling.mask_rules
                 WHERE table_name = $1
                 ORDER BY column_name";
        let rows = client.select(q, None, &[table_name.into()])?;
        for row in rows {
            let col = row
                .get_by_name::<String, _>("column_name")?
                .unwrap_or_default();
            let mt = row
                .get_by_name::<String, _>("mask_type")?
                .unwrap_or_default();
            let mv = row.get_by_name::<String, _>("mask_value")?;
            rules.push(json!({
                "column": col,
                "mask_type": mt,
                "mask_value": mv
            }));
        }
        Ok::<(), pgrx::spi::Error>(())
    });
    pgrx::datum::JsonB(json!({
        "table_name": table_name,
        "action": action_value,
        "rules": rules
    }))
}

/// Apply configured masking rules to a JSONB row.
#[pg_extern]
pub fn cedarling_mask_row(
    row_json: pgrx::datum::JsonB,
    table_name: &str,
    _action: Option<&str>,
) -> pgrx::datum::JsonB {
    let mut out = row_json.0;
    let mut rules: BTreeMap<String, (String, Option<String>)> = BTreeMap::new();
    let _ = Spi::connect(|client| {
        let rows = client.select(
            "SELECT column_name, mask_type, mask_value FROM cedarling.mask_rules WHERE table_name = $1",
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
            rules.insert(col, (mt, mv));
        }
        Ok::<(), pgrx::spi::Error>(())
    });

    if let Some(obj) = out.as_object_mut() {
        for (col, (mask_type, mask_value)) in rules {
            if let Some(existing) = obj.get(&col).cloned() {
                let as_str = match existing {
                    Value::String(s) => Some(s),
                    Value::Null => None,
                    other => Some(other.to_string()),
                };
                let masked = apply_mask(as_str.as_deref(), &mask_type, mask_value.as_deref());
                obj.insert(
                    col,
                    match masked {
                        Some(s) => Value::String(s),
                        None => Value::Null,
                    },
                );
            }
        }
    }
    if let Ok(mut slot) = LAST_MASKED_ROW.lock() {
        *slot = Some(out.clone());
    }
    pgrx::datum::JsonB(out)
}

/// Return the last masked row produced in this backend process.
#[pg_extern]
pub fn cedarling_get_masked_row() -> Option<pgrx::datum::JsonB> {
    LAST_MASKED_ROW
        .lock()
        .ok()
        .and_then(|g| g.clone().map(pgrx::datum::JsonB))
}

fn apply_mask(value: Option<&str>, mask_type: &str, mask_cfg: Option<&str>) -> Option<String> {
    let input = value.unwrap_or_default();
    match mask_type.trim().to_ascii_lowercase().as_str() {
        "null" => None,
        "redact" => Some("***REDACTED***".to_string()),
        "fixed" => Some(mask_cfg.unwrap_or("***").to_string()),
        "partial" => {
            if input.len() <= 4 {
                Some("****".to_string())
            } else {
                let keep = &input[input.len() - 4..];
                Some(format!("****{keep}"))
            }
        },
        "hash" => {
            let mut h = DefaultHasher::new();
            input.hash(&mut h);
            Some(format!("hash:{:016x}", h.finish()))
        },
        "range" => {
            let placeholder = mask_cfg.unwrap_or("0-0");
            Some(placeholder.to_string())
        },
        _ => Some(input.to_string()),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn apply_mask_variants_behave() {
        assert_eq!(
            apply_mask(Some("abcd"), "redact", None),
            Some("***REDACTED***".into())
        );
        assert_eq!(
            apply_mask(Some("abcdef"), "partial", None),
            Some("****cdef".into())
        );
        assert_eq!(apply_mask(Some("abcdef"), "null", None), None);
        assert_eq!(
            apply_mask(Some("abcdef"), "fixed", Some("X")),
            Some("X".into())
        );
    }
}
