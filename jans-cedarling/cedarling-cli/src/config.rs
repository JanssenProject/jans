use anyhow::{Context, Result, bail};
use cedarling::{
    BootstrapConfig, BootstrapConfigRaw, LogTypeConfig, MemoryLogConfig, PolicyStoreSource,
};
use std::fs;
use std::str::FromStr;

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

    let mut config = BootstrapConfig::from_raw_config_and_env(raw)
        .context("failed to resolve bootstrap config from env/file")?;

    // 3: Apply CLI-flag overrides
    if let Some(store) = &args.policy_store {
        let s = store.to_string_lossy();
        let source = if s.starts_with("http://")
            || s.starts_with("https://")
            || s.starts_with("cjar://")
        {
            PolicyStoreSource::Uri(s.to_string())
        } else {
            let ext = store.extension().and_then(|e| e.to_str()).unwrap_or("");
            match ext {
                "json" => PolicyStoreSource::FileJson(store.clone()),
                "yaml" | "yml" => PolicyStoreSource::FileYaml(store.clone()),
                "cjar" => PolicyStoreSource::CjarFile(store.clone()),
                _ => bail!(
                    "unsupported policy store extension: '{}' (must be .json, .yaml, .yml, or .cjar)",
                    ext
                ),
            }
        };
        config.policy_store_config.source = source;
    }

    if let Some(log_type) = &args.log_type {
        config.log_config.log_type = match log_type.to_lowercase().as_str() {
            "off" => LogTypeConfig::Off,
            "memory" => LogTypeConfig::Memory(MemoryLogConfig {
                log_ttl: 3600,
                max_items: None,
                max_item_size: None,
            }),
            "stdout" => LogTypeConfig::StdOut(cedarling::log_config::StdOutLoggerMode::Immediate),
            _ => bail!(
                "invalid log-type: '{}' (must be off, memory, or stdout)",
                log_type
            ),
        };
    }

    if let Some(log_level) = &args.log_level {
        // Try parsing log level directly from cedarling module (could be re-exported differently, but we try log::LogLevel)
        // If it's not exported, we can just deserialize it as JSON!
        let level_str = format!("\"{}\"", log_level.to_uppercase());
        let level = serde_json::from_str(&level_str)
            .map_err(|e| anyhow::anyhow!("invalid log-level: {} ({})", log_level, e))?;
        config.log_config.log_level = level;
    }

    if let Some(app_name) = &args.application_name {
        config.application_name = app_name.clone();
    }

    // 4: Validate policy store source is non-empty
    if let PolicyStoreSource::Yaml(content) = &config.policy_store_config.source {
        if content.contains("policy_stores: {}") || content.is_empty() {
            eprintln!(
                "Warning: Using default empty policy store. Please specify --policy-store or CEDARLING_POLICY_STORE_URI."
            );
        }
    }

    Ok(config)
}
