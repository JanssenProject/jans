// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Shared helpers for `#[pg_test]` modules.

#[cfg(feature = "pg_test")]
pub(crate) use crate::test_fixtures::POLICY_STORE_UNSIGNED_YAML;

#[cfg(feature = "pg_test")]
use std::fs;
#[cfg(feature = "pg_test")]
use std::io::Write;
#[cfg(feature = "pg_test")]
use std::path::{Path, PathBuf};

/// Emit a single-quoted YAML scalar, escaping embedded `'` as `''`.
fn yaml_single_quoted_scalar(value: &str) -> String {
    format!("'{}'", value.replace('\'', "''"))
}

/// Write a minimal bootstrap config wired to a local policy-store file.
///
/// Only the `#[pg_test]` modules need this
#[cfg(feature = "pg_test")]
pub(crate) fn write_bootstrap_yaml(dir: &Path, policy_path: &Path, app_name: &str) -> PathBuf {
    let bootstrap_path = dir.join("bootstrap.yaml");
    let app_name_lit = yaml_single_quoted_scalar(app_name);
    let policy_lit = yaml_single_quoted_scalar(&policy_path.to_string_lossy());
    let contents = format!(
        "CEDARLING_APPLICATION_NAME: {app_name_lit}\n\
         CEDARLING_POLICY_STORE_URI: ''\n\
         CEDARLING_LOG_TYPE: memory\n\
         CEDARLING_LOG_LEVEL: DEBUG\n\
         CEDARLING_LOG_TTL: 60\n\
         CEDARLING_LOCAL_JWKS: null\n\
         CEDARLING_POLICY_STORE_LOCAL: null\n\
         CEDARLING_POLICY_STORE_LOCAL_FN: {policy_lit}\n\
         CEDARLING_JWT_SIG_VALIDATION: disabled\n\
         CEDARLING_JWT_STATUS_VALIDATION: disabled\n\
         CEDARLING_LOCK: disabled\n\
         CEDARLING_LOCK_SERVER_CONFIGURATION_URI: null\n\
         CEDARLING_LOCK_DYNAMIC_CONFIGURATION: disabled\n\
         CEDARLING_LOCK_HEALTH_INTERVAL: 0\n\
         CEDARLING_LOCK_TELEMETRY_INTERVAL: 0\n\
         CEDARLING_LOCK_LISTEN_SSE: disabled\n"
    );
    let mut f = fs::File::create(&bootstrap_path).expect("bootstrap file");
    f.write_all(contents.as_bytes()).expect("write bootstrap");
    bootstrap_path
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn yaml_scalar_quotes_values_with_special_characters() {
        assert_eq!(
            yaml_single_quoted_scalar("app:name#1"),
            "'app:name#1'",
            "colons and hash must be wrapped in quotes"
        );
        assert_eq!(
            yaml_single_quoted_scalar("it's"),
            "'it''s'",
            "embedded single quotes must be doubled"
        );
    }
}
