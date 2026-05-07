// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! SQL-facing policy version helpers (`cedarling_use_policy`, `cedarling_rollback_policy`,
//! `cedarling_diff_policies`).

use std::collections::{BTreeMap, BTreeSet};
use std::fs;

use cedarling::bindings::cedar_policy;
use pgrx::prelude::*;
use serde_json::json;

use crate::authz_cache;
use crate::engine;
use crate::extension_log;
use crate::guc_config;
use crate::policy::PolicyError;

/// Register a named policy version in `cedarling.policy_versions`.
///
/// `cedarling_use_policy` will resolve `name` to `bootstrap_path` before treating the
/// argument as a filesystem path. Returns `true` on success.
#[pg_extern]
pub fn cedarling_register_policy_version(name: &str, bootstrap_path: &str) -> bool {
    let name = name.trim();
    let path = bootstrap_path.trim();
    if name.is_empty() || path.is_empty() {
        extension_log::log_diagnostic(
            guc_config::CedarlingLogLevelGuc::Info,
            "cedarling_register_policy_version: name and bootstrap_path must be non-empty",
        );
        return false;
    }
    let result: Result<(), pgrx::spi::Error> = Spi::connect_mut(|client| {
        client.update(
            "INSERT INTO cedarling.policy_versions (name, bootstrap_path) \
             VALUES ($1, $2) \
             ON CONFLICT (name) DO UPDATE SET bootstrap_path = EXCLUDED.bootstrap_path, \
                                              registered_at  = now()",
            None,
            &[name.into(), path.into()],
        )?;
        Ok(())
    });
    match result {
        Ok(()) => true,
        Err(e) => {
            extension_log::log_diagnostic(
                guc_config::CedarlingLogLevelGuc::Error,
                &format!("cedarling_register_policy_version: {e}"),
            );
            false
        },
    }
}

/// Load and activate a named policy version or bootstrap file path.
///
/// If `version` matches a name in `cedarling.policy_versions`, the registered
/// `bootstrap_path` is used; otherwise the argument is treated as a filesystem path.
/// Clears the authz cache and records the swap in `cedarling.policy_history`.
#[pg_extern]
pub fn cedarling_use_policy(version: &str) -> bool {
    let name_or_path = version.trim();
    if name_or_path.is_empty() {
        extension_log::log_diagnostic(
            guc_config::CedarlingLogLevelGuc::Info,
            "cedarling_use_policy: empty policy path/version",
        );
        return false;
    }

    let resolved = match resolve_bootstrap_path(name_or_path) {
        Ok(r) => r,
        Err(e) => {
            extension_log::log_diagnostic(
                guc_config::CedarlingLogLevelGuc::Error,
                &format!("cedarling_use_policy: registry lookup failed: {e}"),
            );
            ResolvedPolicy {
                bootstrap_path: name_or_path.to_string(),
                version_guc_value: name_or_path.to_string(),
            }
        },
    };

    let previous = match engine::use_policy(&resolved.bootstrap_path) {
        Ok(prev) => prev,
        Err(e) => {
            let pe = PolicyError::Load(e.to_string());
            extension_log::log_diagnostic(guc_config::CedarlingLogLevelGuc::Warn, &pe.to_string());
            return false;
        },
    };

    authz_cache::global_cache().clear_all();
    let _ = set_policy_version_guc(&resolved.version_guc_value);
    let detail = json!({ "result": "ok", "cache_cleared": true });
    let _ = insert_policy_history("use", &resolved.version_guc_value, previous.as_deref(), &detail);
    let _ = trim_policy_history();
    true
}

/// Resolve `name_or_path` via `cedarling.policy_versions`; return it unchanged if not found.
fn resolve_bootstrap_path(name_or_path: &str) -> Result<ResolvedPolicy, pgrx::spi::Error> {
    let row = Spi::get_one_with_args::<String>(
        "SELECT bootstrap_path FROM cedarling.policy_versions WHERE name = $1",
        &[name_or_path.into()],
    )?;
    if let Some(bootstrap_path) = row {
        Ok(ResolvedPolicy {
            bootstrap_path,
            version_guc_value: name_or_path.to_string(),
        })
    } else {
        Ok(ResolvedPolicy {
            bootstrap_path: name_or_path.to_string(),
            version_guc_value: name_or_path.to_string(),
        })
    }
}

struct ResolvedPolicy {
    bootstrap_path: String,
    version_guc_value: String,
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
            let pe = PolicyError::Load(e.to_string());
            extension_log::log_diagnostic(guc_config::CedarlingLogLevelGuc::Warn, &pe.to_string());
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

/// Diff two Cedar policy files by policy id.
///
/// Uses `cedarling.diff_mode`: `structural` (default) parses both files as
/// `cedar_policy::PolicySet` and diffs per-policy-id; `lines` is the legacy
/// line-oriented text diff.
#[pg_extern]
pub fn cedarling_diff_policies(old: &str, new: &str) -> pgrx::datum::JsonB {
    let result = match guc_config::diff_mode() {
        guc_config::CedarlingDiffMode::Structural => diff_policy_files_structural(old, new),
        guc_config::CedarlingDiffMode::Lines => diff_policy_files_lines(old, new),
    };
    match result {
        Ok(v) => pgrx::datum::JsonB(v),
        Err(e) => {
            let pe = PolicyError::Load(e.to_string());
            extension_log::log_diagnostic(guc_config::CedarlingLogLevelGuc::Warn, &pe.to_string());
            pgrx::datum::JsonB(json!({
                "ok": false,
                "error": pe.to_string(),
                "added": [],
                "removed": [],
                "modified": [],
            }))
        },
    }
}

// --- structural diff ---

fn diff_policy_files_structural(
    old: &str,
    new: &str,
) -> Result<serde_json::Value, Box<dyn std::error::Error + Send + Sync>> {
    let old_txt = fs::read_to_string(old)?;
    let new_txt = fs::read_to_string(new)?;
    diff_policy_text_structural(&old_txt, &new_txt)
}

fn diff_policy_text_structural(
    old_txt: &str,
    new_txt: &str,
) -> Result<serde_json::Value, Box<dyn std::error::Error + Send + Sync>> {
    let old_map = policies_by_id(old_txt)?;
    let new_map = policies_by_id(new_txt)?;

    let mut added = Vec::new();
    let mut removed = Vec::new();
    let mut modified = Vec::new();

    for (id, (effect, source)) in &new_map {
        match old_map.get(id) {
            None => added.push(json!({ "id": id, "effect": effect, "source": source })),
            Some((_, old_source)) if old_source != source => modified.push(json!({
                "id": id,
                "effect": effect,
                "old_source": old_source,
                "new_source": source,
            })),
            _ => {},
        }
    }
    for (id, (effect, _)) in &old_map {
        if !new_map.contains_key(id) {
            removed.push(json!({ "id": id, "effect": effect }));
        }
    }

    Ok(json!({
        "ok": true,
        "added": added,
        "removed": removed,
        "modified": modified,
    }))
}

/// Parse Cedar source text and return a map of `policy_id → (effect_str, source_str)`.
fn policies_by_id(
    src: &str,
) -> Result<BTreeMap<String, (String, String)>, Box<dyn std::error::Error + Send + Sync>> {
    let policy_set: cedar_policy::PolicySet = src.parse()?;
    let map = policy_set
        .policies()
        .map(|p| {
            let id = p.id().to_string();
            let effect = match p.effect() {
                cedar_policy::Effect::Permit => "permit",
                cedar_policy::Effect::Forbid => "forbid",
            }
            .to_string();
            let source = p.to_string();
            (id, (effect, source))
        })
        .collect();
    Ok(map)
}

// --- line-based diff (legacy fallback) ---

fn diff_policy_files_lines(
    old: &str,
    new: &str,
) -> Result<serde_json::Value, Box<dyn std::error::Error + Send + Sync>> {
    let old_txt = fs::read_to_string(old)?;
    let new_txt = fs::read_to_string(new)?;
    Ok(diff_policy_text_lines(&old_txt, &new_txt))
}

fn diff_policy_text_lines(old_txt: &str, new_txt: &str) -> serde_json::Value {
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
    fn structural_diff_detects_added_removed_modified() {
        let old = "permit(principal, action, resource) when { resource.x == 1 };";
        let new = "permit(principal, action, resource) when { resource.x == 2 };
                   forbid(principal, action, resource);";
        let d = diff_policy_text_structural(old, new).expect("structural diff");
        assert_eq!(d["ok"], true);
        // policy0 is modified (different when-clause)
        assert!(d["modified"].as_array().is_some_and(|a| !a.is_empty()));
        // forbid policy is added
        assert!(d["added"].as_array().is_some_and(|a| !a.is_empty()));
        assert!(d["removed"].as_array().is_some_and(|a| a.is_empty()));
    }

    #[test]
    fn lines_diff_computes_added_removed() {
        let old = "# comment\na\nb";
        let new = "b\nc";
        let d = diff_policy_text_lines(old, new);
        assert_eq!(d["ok"], true);
        assert!(d["added"].as_array().is_some_and(|a| a.contains(&json!("c"))));
        assert!(d["removed"].as_array().is_some_and(|a| a.contains(&json!("a"))));
    }
}
