use anyhow::{bail, Context, Result};
use cedarling::{Cedarling, RequestUnsigned};
use colored::Colorize;
use std::collections::HashSet;
use std::fs;
use std::path::PathBuf;

use crate::test::spec::{ExpectedDecision, TestFile};

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
            principal: test.request.principal.map(|p| p.into()),
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
                let mut fail_reason = String::new();

                if actual_decision != test.result.decision {
                    test_passed = false;
                    fail_reason = format!(
                        "expected decision {:?}, got {:?}",
                        test.result.decision, actual_decision
                    );
                }

                // Check expected reason_ids (superset match)
                if let Some(expected_ids) = &test.result.reason_ids {
                    let actual_ids: HashSet<String> = result
                        .response
                        .diagnostics()
                        .reason()
                        .map(|id| id.to_string())
                        .collect();

                    for expected_id in expected_ids {
                        if !actual_ids.contains(expected_id) {
                            test_passed = false;
                            fail_reason = format!(
                                "expected reason_id '{}' not found in actual reasons {:?}",
                                expected_id, actual_ids
                            );
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
                        fail_reason = format!(
                            "expected {} errors, got {}",
                            expected_err_count, actual_err_count
                        );
                    }
                }

                if test_passed {
                    println!("{}", "ok".green());
                    passed += 1;
                } else {
                    println!("{} ({})", "fail".red(), fail_reason);
                    failed += 1;
                }
            }
            Err(e) => {
                println!("{} (error: {})", "fail".red(), e);
                failed += 1;
            }
        }
    }

    println!("\nresults: {} passed, {} failed", passed, failed);

    if failed > 0 {
        return Ok(1);
    }

    // Step 6: Coverage report
    let all_metadata = cedarling.all_policy_metadata();
    let total_policies = all_metadata.len();

    if total_policies == 0 {
        println!("Coverage: 0/0 policies (n/a)");
    } else {
        let all_ids: HashSet<String> = all_metadata.into_iter().map(|m| m.id).collect();
        let triggered: HashSet<String> = all_ids.intersection(&reason_ids_seen).cloned().collect();
        let untriggered: HashSet<String> = all_ids.difference(&reason_ids_seen).cloned().collect();
        
        let pct = (triggered.len() as f64 / total_policies as f64) * 100.0;
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
                println!("  - {}", id);
            }
        }
    }

    Ok(0)
}
