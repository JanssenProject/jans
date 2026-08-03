// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use anyhow::{Context, Result};
use cedarling::{Cedarling, LevelResult, PolicyStoreConfig};
use colored::Colorize;

/// Validates a policy store according to parse, schema, and metadata rules.
/// Returns standard exit codes (0 for pass, 1 for fail, 2 for infra error).
///
/// # Errors
/// Returns an error if the policy store validation process fails due to an infrastructure issue (e.g. invalid lock master url, IO error).
pub async fn run(bootstrap: cedarling::BootstrapConfig) -> Result<i32> {
    let source_label = describe_source(&bootstrap.policy_store_config);
    println!("validating {source_label}");

    let report = Cedarling::validate_policy_store(&bootstrap.policy_store_config)
        .await
        .context("failed to run policy store validation")?;

    print_level("parse   ", &report.parse);
    print_level("schema  ", &report.schema);
    print_level("metadata", &report.metadata);

    if report.is_ok() {
        println!("validation passed");
        Ok(0)
    } else {
        let n = report.error_count();
        println!("validation failed: {n} error(s)");
        Ok(1)
    }
}

fn print_level(name: &str, result: &LevelResult) {
    let dots = ".".repeat(24usize.saturating_sub(name.len()));
    match result {
        LevelResult::Ok => println!("  {name} {dots} {}", "OK".green()),
        LevelResult::Skipped { reason } => println!("  {name} {dots} skipped ({reason})"),
        LevelResult::Failed { errors } => {
            println!("  {name} {dots} {}", "FAIL".red());
            for e in errors {
                let loc = match (e.line, e.column) {
                    (Some(l), Some(c)) => format!("{}:{l}:{c}", e.file),
                    (Some(l), None) => format!("{}:{l}", e.file),
                    _ => e.file.clone(),
                };
                println!("    {loc}: {}", e.message);
            }
        },
    }
}

fn describe_source(cfg: &PolicyStoreConfig) -> String {
    use cedarling::PolicyStoreSource;
    match &cfg.source {
        PolicyStoreSource::FileJson(path)
        | PolicyStoreSource::FileYaml(path)
        | PolicyStoreSource::CjarFile(path) => path.display().to_string(),
        PolicyStoreSource::Directory(path) => format!("{}/", path.display()),
        PolicyStoreSource::Uri(url)
        | PolicyStoreSource::CjarUrl(url)
        | PolicyStoreSource::LockServer(url) => url.clone(),
        PolicyStoreSource::Json(_) => "<inline json>".to_string(),
        PolicyStoreSource::Yaml(_) => "<inline yaml>".to_string(),
        PolicyStoreSource::ArchiveBytes(_) => "<inline bytes>".to_string(),
    }
}
