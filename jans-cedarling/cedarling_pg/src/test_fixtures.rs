// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Shared policy-store fixtures for unit tests and `#[pg_test]`.

/// Unsigned policy store used across integration and bridge unit tests.
pub(crate) const POLICY_STORE_UNSIGNED_YAML: &str = include_str!(concat!(
    env!("CARGO_MANIFEST_DIR"),
    "/../test_files/policy-store_no_trusted_issuers.yaml"
));
