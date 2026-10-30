// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use assert_cmd::Command;

#[test]
fn test_integration_pass() {
    let mut cmd = Command::cargo_bin("cedarling-cli").unwrap();
    cmd.env("CEDARLING_JWT_SIG_VALIDATION", "disabled")
        .arg("--policy-store")
        .arg("tests/test_store.yaml")
        .arg("test")
        .arg("--test-file")
        .arg("tests/tests.yaml")
        .assert()
        .success()
        .stdout(predicates::str::contains("results: 2 passed, 0 failed"));
}

#[test]
fn test_integration_fail() {
    let mut cmd = Command::cargo_bin("cedarling-cli").unwrap();
    cmd.env("CEDARLING_JWT_SIG_VALIDATION", "disabled")
        .arg("--policy-store")
        .arg("tests/test_store.yaml")
        .arg("test")
        .arg("--test-file")
        .arg("tests/tests_failing.yaml");

    cmd.assert()
        .failure()
        .code(1)
        .stdout(predicates::str::contains("results: 0 passed, 1 failed"));
}

#[test]
fn test_validate_pass() {
    let mut cmd = Command::cargo_bin("cedarling-cli").unwrap();
    cmd.env("CEDARLING_JWT_SIG_VALIDATION", "disabled")
        .arg("--policy-store")
        .arg("tests/test_store.yaml")
        .arg("validate");

    cmd.assert()
        .success()
        .stdout(predicates::str::contains("schema .................. OK"));
}

#[test]
fn test_validate_fail() {
    let mut cmd = Command::cargo_bin("cedarling-cli").unwrap();
    cmd.env("CEDARLING_JWT_SIG_VALIDATION", "disabled")
        .arg("--policy-store")
        .arg("tests/test_store_schema_error.yaml")
        .arg("validate");

    cmd.assert()
        .failure()
        .code(1)
        .stdout(predicates::str::contains("schema .................. FAIL"));
}

#[test]
fn test_validate_reports_unknown_fields() {
    let mut cmd = Command::cargo_bin("cedarling-cli").unwrap();
    cmd.env("CEDARLING_JWT_SIG_VALIDATION", "disabled")
        .arg("--policy-store")
        .arg(concat!(
            env!("CARGO_MANIFEST_DIR"),
            "/tests/v2_store_unknown_field"
        ))
        .arg("validate");

    cmd.assert()
        .success()
        .stdout(predicates::str::contains(
            "trusted-issuers.json: unknown field `Acme::Access_token.required_claim` is ignored; \
             did you mean `required_claims`?",
        ))
        .stdout(predicates::str::contains(
            "unknown field `Acme::Access_token.x_vendor` is ignored\n",
        ))
        .stdout(predicates::str::contains("validation passed"));
}

#[test]
fn test_validate_strict_fails_on_unknown_fields() {
    let mut cmd = Command::cargo_bin("cedarling-cli").unwrap();
    cmd.env("CEDARLING_JWT_SIG_VALIDATION", "disabled")
        .arg("--policy-store")
        .arg(concat!(
            env!("CARGO_MANIFEST_DIR"),
            "/tests/v2_store_unknown_field"
        ))
        .arg("validate")
        .arg("--strict");

    cmd.assert()
        .failure()
        .code(1)
        .stdout(predicates::str::contains("2 unknown field(s) with --strict"));
}

#[test]
fn test_validate_accepts_relative_directory() {
    let mut cmd = Command::cargo_bin("cedarling-cli").unwrap();
    cmd.env("CEDARLING_JWT_SIG_VALIDATION", "disabled")
        .arg("--policy-store")
        .arg("./tests/v2_store_unknown_field/")
        .arg("validate");

    cmd.assert()
        .success()
        .stdout(predicates::str::contains("validation passed"));
}

#[test]
fn test_validate_missing_source() {
    let mut cmd = Command::cargo_bin("cedarling-cli").unwrap();
    cmd.env("CEDARLING_JWT_SIG_VALIDATION", "disabled")
        .arg("validate");

    cmd.assert()
        .failure()
        .code(2)
        .stderr(predicates::str::contains(
            "failed to resolve bootstrap config from env/file",
        ));
}

#[test]
fn test_authorize_allow() {
    let mut cmd = Command::cargo_bin("cedarling-cli").unwrap();
    cmd.env("CEDARLING_JWT_SIG_VALIDATION", "disabled")
        .arg("--policy-store")
        .arg("tests/test_store.yaml")
        .arg("authorize")
        .arg("--principal-type")
        .arg("Jans::Role")
        .arg("--principal-id")
        .arg("SuperUser")
        .arg("--action")
        .arg("Jans::Action::\"Update\"")
        .arg("--resource-type")
        .arg("Jans::Issue")
        .arg("--resource-id")
        .arg("1")
        .arg("--resource-attrs")
        .arg("{\"country\": \"US\", \"org_id\": \"org1\"}")
        .arg("--context")
        .arg("{}");

    cmd.assert()
        .success()
        .stdout(predicates::str::contains("allow"));
}

#[test]
fn test_authorize_deny() {
    let mut cmd = Command::cargo_bin("cedarling-cli").unwrap();
    cmd.env("CEDARLING_JWT_SIG_VALIDATION", "disabled")
        .arg("--policy-store")
        .arg("tests/test_store.yaml")
        .arg("authorize")
        .arg("--principal-type")
        .arg("Jans::Role")
        .arg("--principal-id")
        .arg("NormalUser")
        .arg("--action")
        .arg("Jans::Action::\"Update\"")
        .arg("--resource-type")
        .arg("Jans::Issue")
        .arg("--resource-id")
        .arg("1")
        .arg("--resource-attrs")
        .arg("{\"country\": \"US\", \"org_id\": \"org1\"}")
        .arg("--context")
        .arg("{}");

    cmd.assert()
        .failure()
        .code(1)
        .stdout(predicates::str::contains("deny"));
}

#[test]
fn test_authorize_malformed_json() {
    let mut cmd = Command::cargo_bin("cedarling-cli").unwrap();
    cmd.env("CEDARLING_JWT_SIG_VALIDATION", "disabled")
        .arg("--policy-store")
        .arg("tests/test_store.yaml")
        .arg("authorize")
        .arg("--principal-type")
        .arg("Jans::Role")
        .arg("--principal-id")
        .arg("SuperUser")
        .arg("--action")
        .arg("Jans::Action::\"Update\"")
        .arg("--resource-type")
        .arg("Jans::Issue")
        .arg("--resource-id")
        .arg("1")
        .arg("--resource-attrs")
        .arg("{\"country\": \"US\", \"org_id\": \"org1\"}")
        .arg("--context")
        .arg("{malformed}");

    cmd.assert()
        .failure()
        .code(2)
        .stderr(predicates::str::contains("failed to parse context as JSON"));
}
