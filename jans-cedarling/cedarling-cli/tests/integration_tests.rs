use assert_cmd::Command;

#[test]
fn test_integration_pass() {
    let mut cmd = Command::cargo_bin("cedarling_cli").unwrap();
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
