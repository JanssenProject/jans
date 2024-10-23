/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::policy_store::{load_policy_store, LoadPolicyStoreError};
use crate::PolicyStoreConfig;

/// Tests the behavior of the `load_policy_store` function when multiple role mappings
/// are defined in the policy store configuration.
///
/// This test verifies that:
/// 1. An error is returned when the policy store JSON contains multiple tokens with
///    a `role_mapping` field.
/// 2. The specific error returned is of type `LoadPolicyStoreError::MultipleRoleMappings`
///    with the correct token kinds included in the error message.
///
/// The test uses a sample JSON file (`policy-store_with_multiple_role_mappings_err.json`)
/// that is expected to trigger this error scenario.
#[test]
fn errors_on_multiple_role_mappings() {
    static POLICY_STORE_RAW: &str =
        include_str!("../../../test_files/policy-store_with_multiple_role_mappings_err.json");
    let config = PolicyStoreConfig {
        source: crate::PolicyStoreSource::Json(POLICY_STORE_RAW.to_string()),
        store_id: None,
    };
    let result = load_policy_store(&config);

    assert!(result.is_err());
    assert!(matches!(
        result,
        Err(LoadPolicyStoreError::MultipleRoleMappings(
            ref token_kinds
        )) if token_kinds == "id, userinfo"
    ));
}
