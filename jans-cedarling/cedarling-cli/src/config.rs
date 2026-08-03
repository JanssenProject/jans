// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use anyhow::{Context, Result};
use cedarling::{BootstrapConfig, BootstrapConfigRaw};
use std::fs;

use crate::cli::CommonArgs;

/// Resolves the bootstrap configuration from CLI arguments and environment variables.
///
/// # Errors
///
/// Returns an error if the policy store conflicts (e.g., cannot load multiple paths), or if the JSON parsing of the `log_type` fails, or if `BootstrapConfigRaw::try_into()` fails.
pub fn resolve_bootstrap(args: &CommonArgs) -> Result<BootstrapConfig> {
    // 1 & 2: Load raw config from file if provided, otherwise env fallback happens inside from_raw_config_and_env
    let raw = if let Some(path) = &args.config {
        let content = fs::read_to_string(path)
            .with_context(|| format!("failed to read config file from {}", path.display()))?;

        let ext = path.extension().and_then(|e| e.to_str()).unwrap_or("");
        let parsed: BootstrapConfigRaw = if ext == "yaml" || ext == "yml" {
            serde_yaml_ng::from_str(&content).context("failed to parse config as YAML")?
        } else {
            serde_json::from_str(&content).context("failed to parse config as JSON")?
        };
        Some(parsed)
    } else {
        None
    };

    let mut raw_config = BootstrapConfigRaw::from_raw_config_and_env(raw)
        .context("failed to merge env and file config")?;

    // 3: Apply CLI-flag overrides to raw_config BEFORE try_into validation
    if let Some(store) = &args.policy_store {
        // Clear all conflicting sources before overriding
        raw_config.local_policy_store = None;
        raw_config.policy_store_uri = None;
        raw_config.policy_store_local_fn = None;
        raw_config.policy_store_cjar_url = None;

        let s = store.to_string_lossy().to_string();
        if s.starts_with("http://") || s.starts_with("https://") {
            raw_config.policy_store_uri = Some(s);
        } else if s.starts_with("cjar://") {
            // Strip the pseudocheme before storing it as a real URL
            raw_config.policy_store_cjar_url =
                Some(s.strip_prefix("cjar://").unwrap_or(&s).to_string());
        } else {
            raw_config.policy_store_local_fn = Some(s);
        }
    }

    if let Some(log_type) = &args.log_type {
        let json_str = serde_json::to_string(&log_type.to_lowercase())
            .expect("string serialization cannot fail");
        raw_config.log_type = serde_json::from_str(&json_str)
            .map_err(|e| anyhow::anyhow!("invalid log-type: {log_type} ({e})"))?;
    }

    if let Some(log_level) = &args.log_level {
        let json_str = serde_json::to_string(&log_level.to_uppercase())
            .expect("string serialization cannot fail");
        raw_config.log_level = serde_json::from_str(&json_str)
            .map_err(|e| anyhow::anyhow!("invalid log-level: {log_level} ({e})"))?;
    }

    if let Some(app_name) = &args.application_name {
        raw_config.application_name.clone_from(app_name);
    }

    // 4: Validate and convert to BootstrapConfig
    let config: BootstrapConfig = raw_config
        .try_into()
        .context("failed to resolve bootstrap config from env/file")?;

    Ok(config)
}
