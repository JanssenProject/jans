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
/// Returns an error if the policy store conflicts (e.g., cannot load multiple paths),
/// or if `BootstrapConfigRaw::try_into()` fails.
pub fn resolve_bootstrap(args: &CommonArgs) -> Result<BootstrapConfig> {
    // 1 & 2: Load raw config from file if provided, otherwise env fallback happens inside from_raw_config_and_env
    let raw = if let Some(path) = &args.config {
        let content = fs::read_to_string(path)
            .with_context(|| format!("failed to read config file from {}", path.display()))?;

        let ext = path
            .extension()
            .and_then(|e| e.to_str())
            .unwrap_or("")
            .to_lowercase();
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
    if let Some(cjar_url) = &args.policy_store_cjar_url {
        raw_config.local_policy_store = None;
        raw_config.policy_store_uri = None;
        raw_config.policy_store_local_fn = None;
        raw_config.policy_store_cjar_url = Some(cjar_url.clone());
    } else if let Some(store) = &args.policy_store {
        // Clear all conflicting sources before overriding
        raw_config.local_policy_store = None;
        raw_config.policy_store_uri = None;
        raw_config.policy_store_local_fn = None;
        raw_config.policy_store_cjar_url = None;

        let s = store.to_string_lossy().to_string();
        if s.starts_with("http://") || s.starts_with("https://") {
            raw_config.policy_store_uri = Some(s);
        } else {
            raw_config.policy_store_local_fn = Some(s);
        }
    }

    if let Some(log_type) = args.log_type {
        raw_config.log_type = log_type;
    }

    if let Some(log_level) = args.log_level {
        raw_config.log_level = log_level;
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

#[cfg(test)]
mod tests {
    use super::*;
    use cedarling::PolicyStoreSource;
    use std::{
        env,
        path::PathBuf,
        sync::{LazyLock, Mutex},
    };

    static ENV_MUTEX: LazyLock<Mutex<()>> = LazyLock::new(|| Mutex::new(()));

    /// Runs `test` with only `vars` set in the environment, then restores the
    /// original environment. Serialized via `ENV_MUTEX` since env vars are
    /// process-global and tests otherwise run concurrently.
    fn with_env_vars<F: FnOnce()>(vars: &[(&str, &str)], test: F) {
        let _lock = ENV_MUTEX.lock().unwrap();

        let original: Vec<(String, String)> = env::vars().collect();
        for (key, _) in &original {
            unsafe { env::remove_var(key) };
        }

        for (key, value) in vars {
            unsafe { env::set_var(key, value) };
        }

        test();

        for (key, _) in vars {
            unsafe { env::remove_var(key) };
        }
        for (key, value) in original {
            unsafe { env::set_var(key, value) };
        }
    }

    fn dummy_args() -> CommonArgs {
        CommonArgs {
            config: None,
            policy_store: None,
            policy_store_cjar_url: None,
            log_type: None,
            log_level: None,
            application_name: None,
            no_color: false,
        }
    }

    #[test]
    fn test_override_policy_store_cjar() {
        with_env_vars(&[("CEDARLING_JWT_SIG_VALIDATION", "disabled")], || {
            let mut args = dummy_args();
            args.policy_store_cjar_url = Some("https://example.com/store.cjar".to_string());

            let config = resolve_bootstrap(&args).expect("should resolve successfully");
            assert!(matches!(
                config.policy_store_config.source,
                PolicyStoreSource::CjarUrl(s) if s == "https://example.com/store.cjar"
            ));
        });
    }

    #[test]
    fn test_override_policy_store_http() {
        with_env_vars(&[("CEDARLING_JWT_SIG_VALIDATION", "disabled")], || {
            let mut args = dummy_args();
            args.policy_store = Some(PathBuf::from("http://example.com/store.yaml"));

            let config = resolve_bootstrap(&args).expect("should resolve successfully");
            assert!(matches!(
                config.policy_store_config.source,
                PolicyStoreSource::Uri(s) if s == "http://example.com/store.yaml"
            ));
        });
    }

    #[test]
    fn test_override_application_name() {
        with_env_vars(
            &[
                ("CEDARLING_JWT_SIG_VALIDATION", "disabled"),
                ("CEDARLING_POLICY_STORE_LOCAL", "tests/test_store.yaml"),
            ],
            || {
                let mut args = dummy_args();
                args.application_name = Some("CLI_APP".to_string());

                let config = resolve_bootstrap(&args).expect("should resolve successfully");
                assert_eq!(config.application_name, "CLI_APP");
            },
        );
    }
}
