use anyhow::{Context, Result};
use cedarling::{BootstrapConfig, BootstrapConfigRaw};
use std::fs;

use crate::cli::CommonArgs;

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
        let s = store.to_string_lossy().to_string();
        if s.starts_with("http://")
            || s.starts_with("https://")
            || s.starts_with("cjar://")
        {
            raw_config.policy_store_uri = Some(s);
        } else {
            raw_config.policy_store_local_fn = Some(s);
        }
    }

    if let Some(log_type) = &args.log_type {
        let json_str = format!("\"{}\"", log_type.to_uppercase());
        raw_config.log_type = serde_json::from_str(&json_str)
            .map_err(|e| anyhow::anyhow!("invalid log-type: {} ({})", log_type, e))?;
    }

    if let Some(log_level) = &args.log_level {
        let json_str = format!("\"{}\"", log_level.to_uppercase());
        raw_config.log_level = serde_json::from_str(&json_str)
            .map_err(|e| anyhow::anyhow!("invalid log-level: {} ({})", log_level, e))?;
    }

    if let Some(app_name) = &args.application_name {
        raw_config.application_name = app_name.clone();
    }

    // 4: Validate and convert to BootstrapConfig
    let config: BootstrapConfig = raw_config
        .try_into()
        .context("failed to resolve bootstrap config from env/file")?;

    Ok(config)
}
