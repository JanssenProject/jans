// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedar_policy_core::validator::ValidatorSchema;

pub(crate) mod cedar_json;
pub(crate) const CEDAR_NAMESPACE_SEPARATOR: &str = "::";

/// Box that holds the [`cedar_policy::Schema`] and
/// JSON representation that is used to create entities from the schema in the policy store.
#[derive(Debug, Clone)]
pub struct CedarSchema {
    pub schema: cedar_policy::Schema,
    pub json: cedar_json::CedarSchemaJson,
    pub validator_schema: ValidatorSchema,
}

#[cfg(test)]
impl PartialEq for CedarSchema {
    fn eq(&self, other: &Self) -> bool {
        self.json == other.json
    }
}

#[cfg(test)]
mod tests {
    use test_utils::assert_eq;

    use crate::common::policy_store::legacy_store::{LegacyAgamaPolicyStore, LegacyPolicyStore};

    #[test]
    fn test_read_ok() {
        static POLICY_STORE_RAW: &str =
            include_str!("../../../../test_files/policy-store_ok.yaml");
        serde_yml::from_str::<LegacyAgamaPolicyStore>(POLICY_STORE_RAW)
            .expect("failed to deserialize policy-store_ok.yaml into LegacyAgamaPolicyStore");
    }

    #[test]
    fn test_agama_ok() {
        static POLICY_STORE_RAW: &str = include_str!("../../../../test_files/agama-store.yaml");
        serde_yml::from_str::<LegacyAgamaPolicyStore>(POLICY_STORE_RAW)
            .expect("failed to deserialize agama-store.yaml into LegacyAgamaPolicyStore");
    }

    #[test]
    fn test_readable_json_ok() {
        static POLICY_STORE_RAW: &str =
            include_str!("../../../../test_files/policy-store_readable.json");
        serde_json::from_str::<LegacyAgamaPolicyStore>(POLICY_STORE_RAW)
            .expect("failed to deserialize policy-store_readable.json into LegacyAgamaPolicyStore");
    }

    #[test]
    fn test_readable_yaml_ok() {
        static YAML_POLICY_STORE: &str =
            include_str!("../../../../test_files/policy-store_readable.yaml");
        serde_yml::from_str::<LegacyAgamaPolicyStore>(YAML_POLICY_STORE)
            .expect("failed to deserialize policy-store_readable.yaml into LegacyAgamaPolicyStore");
    }

    #[test]
    fn test_readable_yaml_identical_readable_json() {
        static YAML_POLICY_STORE: &str =
            include_str!("../../../../test_files/policy-store_readable.yaml");
        static JSON_POLICY_STORE: &str =
            include_str!("../../../../test_files/policy-store_readable.json");

        let yaml_policy_result =
            serde_yml::from_str::<LegacyAgamaPolicyStore>(YAML_POLICY_STORE);
        let json_policy_result =
            serde_json::from_str::<LegacyAgamaPolicyStore>(JSON_POLICY_STORE);

        assert_eq!(yaml_policy_result.unwrap(), json_policy_result.unwrap());
    }

    #[test]
    fn test_both_ok() {
        static POLICY_STORE_RAW: &str =
            include_str!("../../../../test_files/policy-store_blobby.json");
        let policy_result = serde_json::from_str::<LegacyPolicyStore>(POLICY_STORE_RAW);
        let err = policy_result.expect_err("expected parsing to fail due to missing required field 'name'");
        let msg = err.to_string();
        assert!(msg.contains("missing required field 'name' in policy store entry"), "expected error to mention 'missing required field name' but got: {msg}");
    }

    #[test]
    fn test_read_base64_error() {
        static POLICY_STORE_RAW: &str =
            include_str!("../../../../test_files/policy-store_schema_err_base64.json");
        let policy_result = serde_json::from_str::<LegacyAgamaPolicyStore>(POLICY_STORE_RAW);
        let err = policy_result.expect_err("expected parsing to fail due to missing required field 'name'");
        let msg = err.to_string();
        assert!(msg.contains("missing required field 'name' in policy store entry"), "expected error to mention 'missing required field name' but got: {msg}");
    }

    #[test]
    fn test_read_json_error() {
        static POLICY_STORE_RAW_YAML: &str =
            include_str!("../../../../test_files/policy-store_schema_err.yaml");
        let policy_result =
            serde_yml::from_str::<LegacyAgamaPolicyStore>(POLICY_STORE_RAW_YAML);
        let err = policy_result.expect_err("expected parsing to fail due to missing required field 'name'");
        let msg = err.to_string();
        assert!(msg.contains("missing required field 'name' in policy store entry"), "expected error to mention 'missing required field name' but got: {msg}");
    }

    #[test]
    fn test_parse_cedar_error() {
        static POLICY_STORE_RAW_YAML: &str =
            include_str!("../../../../test_files/policy-store_schema_err_cedar_mistake.yaml");
        let policy_result =
            serde_yml::from_str::<LegacyAgamaPolicyStore>(POLICY_STORE_RAW_YAML);
        let err_msg = policy_result.expect_err("expected parsing to fail due to missing required field 'name'").to_string();
        assert_eq!(
            err_msg,
            "error parsing policy store 'a1bf93115de86de760ee0bea1d529b521489e5a11747': missing required field 'name' in policy store entry"
        );
    }
}
