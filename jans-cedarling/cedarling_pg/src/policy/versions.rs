// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! SQL-facing policy version helpers (`cedarling_use_policy`, `cedarling_rollback_policy`,
//! `cedarling_diff_policies`).

use std::collections::BTreeSet;
use std::fs;

use pgrx::prelude::*;
use serde_json::json;

use crate::authz_cache;
use crate::engine;
use crate::error::CedarlingError;
use crate::extension_log;
use crate::guc_config;

/// Load and activate a policy/bootstrap file path for the current backend process.
///
/// This swaps the process-local Cedarling engine slot and clears the local authz cache.
/// Returns `true` on success, `false` on any load/swap failure.
#[pg_extern]
pub fn cedarling_use_policy(version: &str) -> bool {
    let new_path = version.trim();
    if new_path.is_empty() {
        extension_log::log_diagnostic(
            crate::guc_config::CedarlingLogLevelGuc::Info,
            "cedarling_use_policy: empty policy path/version",
        );
        return false;
    }

    let previous = match engine::use_policy(new_path) {
        Ok(prev) => prev,
        Err(e) => {
            let ce = CedarlingError::PolicyLoading(e.to_string());
            extension_log::log_diagnostic(ce.log_level(), &ce.to_string());
            return false;
        },
    };

    authz_cache::global_cache().clear_all();
    let _ = set_policy_version_guc(new_path);
    let detail = json!({
        "result": "ok",
        "cache_cleared": true,
    });
    let _ = insert_policy_history("use", new_path, previous.as_deref(), &detail);
    let _ = trim_policy_history();
    true
}

/// Roll back to the previous policy loaded via [`cedarling_use_policy`].
///
/// Returns `true` when a rollback happened; returns `false` if there is no previous policy slot
/// or if rollback fails.
#[pg_extern]
pub fn cedarling_rollback_policy() -> bool {
    let rolled = match engine::rollback_policy() {
        Ok(v) => v,
        Err(e) => {
            let ce = CedarlingError::PolicyLoading(e.to_string());
            extension_log::log_diagnostic(ce.log_level(), &ce.to_string());
            return false;
        },
    };

    let Some((rolled_to, rolled_from)) = rolled else {
        extension_log::log_diagnostic(
            crate::guc_config::CedarlingLogLevelGuc::Info,
            "cedarling_rollback_policy: no previous policy to roll back to",
        );
        return false;
    };

    authz_cache::global_cache().clear_all();
    let _ = set_policy_version_guc(&rolled_to);
    let detail = json!({
        "result": "ok",
        "rolled_back_from": rolled_from,
        "cache_cleared": true,
    });
    let _ = insert_policy_history("rollback", &rolled_to, Some(&rolled_from), &detail);
    let _ = trim_policy_history();
    true
}

/// Return a structural text diff between two policy files.
///
/// The diff is line-oriented and intentionally conservative: we return unique non-empty lines
/// present only in `new` (`added`) and only in `old` (`removed`).
#[pg_extern]
pub fn cedarling_diff_policies(old: &str, new: &str) -> pgrx::datum::JsonB {
    match diff_policy_files(old, new) {
        Ok(v) => pgrx::datum::JsonB(v),
        Err(e) => {
            let ce = CedarlingError::PolicyLoading(e.to_string());
            extension_log::log_diagnostic(ce.log_level(), &ce.to_string());
            pgrx::datum::JsonB(json!({
                "ok": false,
                "error": ce.to_string(),
                "added": [],
                "removed": [],
                "modified": [],
            }))
        },
    }
}

fn diff_policy_files(
    old: &str,
    new: &str,
) -> Result<serde_json::Value, Box<dyn std::error::Error + Send + Sync>> {
    let old_txt = fs::read_to_string(old)?;
    let new_txt = fs::read_to_string(new)?;
    Ok(diff_policy_text(&old_txt, &new_txt))
}

fn diff_policy_text(old_txt: &str, new_txt: &str) -> serde_json::Value {
    let old_set = normalized_lines(old_txt);
    let new_set = normalized_lines(new_txt);
    let added: Vec<String> = new_set.difference(&old_set).cloned().collect();
    let removed: Vec<String> = old_set.difference(&new_set).cloned().collect();
    json!({
        "ok": true,
        "added": added,
        "removed": removed,
        "modified": [],
    })
}

fn normalized_lines(src: &str) -> BTreeSet<String> {
    src.lines()
        .map(str::trim)
        .filter(|line| !line.is_empty() && !line.starts_with('#'))
        .map(ToOwned::to_owned)
        .collect()
}

fn set_policy_version_guc(version: &str) -> Result<(), pgrx::spi::Error> {
    Spi::connect_mut(|client| {
        client.update(
            "SELECT set_config('cedarling.policy_version', $1, false)",
            None,
            &[version.into()],
        )?;
        Ok(())
    })
}

fn insert_policy_history(
    operation: &str,
    version: &str,
    previous: Option<&str>,
    detail: &serde_json::Value,
) -> Result<(), pgrx::spi::Error> {
    let detail_str = serde_json::to_string(detail).unwrap_or_else(|_| "{}".to_string());
    Spi::connect_mut(|client| {
        client.update(
            "INSERT INTO cedarling.policy_history (version, previous, operation, detail) VALUES ($1, $2, $3, $4::jsonb)",
            None,
            &[version.into(), previous.into(), operation.into(), detail_str.into()],
        )?;
        Ok(())
    })
}

fn trim_policy_history() -> Result<(), pgrx::spi::Error> {
    let keep_rows = guc_config::policy_history_size();
    Spi::connect_mut(|client| {
        if keep_rows <= 0 {
            client.update("DELETE FROM cedarling.policy_history", None, &[])?;
            return Ok(());
        }
        client.update(
            "DELETE FROM cedarling.policy_history
             WHERE id NOT IN (
               SELECT id
               FROM cedarling.policy_history
               ORDER BY applied_at DESC, id DESC
               LIMIT $1
             )",
            None,
            &[keep_rows.into()],
        )?;
        Ok(())
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn diff_policy_text_computes_added_removed_lines() {
        let old = r#"
            # comment
            a
            b
        "#;
        let new = r#"
            b
            c
        "#;
        let d = diff_policy_text(old, new);
        assert_eq!(d["ok"], true);
        assert!(d["added"]
            .as_array()
            .is_some_and(|a| a.contains(&json!("c"))));
        assert!(d["removed"]
            .as_array()
            .is_some_and(|a| a.contains(&json!("a"))));
    }
}
