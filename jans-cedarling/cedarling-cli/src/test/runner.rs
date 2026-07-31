use anyhow::{bail, Context, Result};
use cedarling::{Cedarling, RequestUnsigned};
use colored::Colorize;
use std::collections::HashSet;
use std::fs;
use std::path::PathBuf;

use crate::test::spec::{ExpectedDecision, TestFile};

/// Runs the tests specified in the YAML test file against the provided configuration.
///
/// # Errors
///
/// Returns an error if the YAML test file cannot be read, parsed, or if any evaluation assertions fail to build context.
pub async fn run(
    config: cedarling::BootstrapConfig,
    test_file_path: PathBuf,
) -> Result<i32> {
    let cedarling = Cedarling::new(&config)
        .await
        .context("failed to initialize Cedarling")?;

    let content = fs::read_to_string(&test_file_path)
        .with_context(|| format!("failed to read test file: {}", test_file_path.display()))?;

    let test_file: TestFile = serde_yaml_ng::from_str(&content)
        .with_context(|| format!("failed to parse YAML in {}", test_file_path.display()))?;

    if test_file.tests.is_empty() {
        bail!("test file contains no tests");
    }

    println!("running {} test(s)", test_file.tests.len());

    let mut passed = 0;
    let mut failed = 0;
    let mut reason_ids_seen = HashSet::new();

    for test in test_file.tests {
        print!("  test {} ... ", test.name);

        let req = RequestUnsigned {
            principal: test.request.principal.map(std::convert::Into::into),
            action: test.request.action,
            resource: test.request.resource.into(),
            context: test.request.context,
        };

        match cedarling.authorize_unsigned(req).await {
            Ok(result) => {
                let actual_decision = if result.decision {
                    ExpectedDecision::Allow
                } else {
                    ExpectedDecision::Deny
                };

                let mut test_passed = true;
                let mut fail_reasons = Vec::new();

                if actual_decision != test.result.decision {
                    test_passed = false;
                    fail_reasons.push(format!(
                        "expected decision {:?}, got {:?}",
                        test.result.decision, actual_decision
                    ));
                }

                // Check expected reason_ids (superset match)
                if let Some(expected_ids) = &test.result.reason_ids {
                    let actual_ids: HashSet<String> = result
                        .response
                        .diagnostics()
                        .reason()
                        .map(std::string::ToString::to_string)
                        .collect();

                    for expected_id in expected_ids {
                        if !actual_ids.contains(expected_id) {
                            test_passed = false;
                            fail_reasons.push(format!(
                                "expected reason_id '{expected_id}' not found in actual reasons {actual_ids:?}"
                            ));
                            break;
                        }
                    }
                }

                // Collect reason IDs seen
                for id in result.response.diagnostics().reason() {
                    reason_ids_seen.insert(id.to_string());
                }

                // Check expected num_errors
                if let Some(expected_err_count) = test.result.num_errors {
                    let actual_err_count = result.response.diagnostics().errors().count();
                    if expected_err_count != actual_err_count {
                        test_passed = false;
                        fail_reasons.push(format!(
                            "expected {expected_err_count} errors, got {actual_err_count}"
                        ));
                    }
                }

                if test_passed {
                    println!("{}", "ok".green());
                    passed += 1;
                } else {
                    println!("{} ({})", "fail".red(), fail_reasons.join("; "));
                    failed += 1;
                }
            }
            Err(e) => {
                println!("{} (error: {})", "fail".red(), e);
                failed += 1;
            }
        }
    }

    println!("\nresults: {passed} passed, {failed} failed");

    if failed > 0 {
        return Ok(1);
    }

    // Step 6: Coverage report
    print_coverage(&cedarling, &reason_ids_seen);

    Ok(0)
}

fn print_coverage(cedarling: &Cedarling, reason_ids_seen: &HashSet<String>) {
    let all_metadata = cedarling.all_policy_metadata();
    let total_policies = all_metadata.len();

    if total_policies == 0 {
        println!("Coverage: 0/0 policies (n/a)");
        return;
    }

    let all_ids: HashSet<String> = all_metadata.into_iter().map(|m| m.id).collect();
    let triggered: HashSet<String> = all_ids.intersection(reason_ids_seen).cloned().collect();
    let untriggered: HashSet<String> = all_ids.difference(reason_ids_seen).cloned().collect();

    // Use safe casting to avoid clippy::cast_precision_loss
    let triggered_count = u32::try_from(triggered.len()).unwrap_or(u32::MAX);
    let total_count = u32::try_from(total_policies).unwrap_or(u32::MAX);
    let pct = (f64::from(triggered_count) / f64::from(total_count)) * 100.0;
    
    println!("Coverage: {}/{} policies triggered ({:.2}%)", triggered.len(), total_policies, pct);

    let mut triggered_sorted: Vec<_> = triggered.into_iter().collect();
    triggered_sorted.sort();
    for id in triggered_sorted {
        println!("  {} {}", "✓".green(), id);
    }

    let mut untriggered_sorted: Vec<_> = untriggered.into_iter().collect();
    untriggered_sorted.sort();
    for id in untriggered_sorted {
        println!("  {} {}", "✗".red(), id);
    }

    let mut extra_ids: Vec<_> = reason_ids_seen.difference(&all_ids).collect();
    if !extra_ids.is_empty() {
        extra_ids.sort();
        println!("{}", "Warning: The following policies were seen in test expected results but do not exist in the policy store:".yellow());
        for id in extra_ids {
            println!("  - {id}");
        }
    }
}
