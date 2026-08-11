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
pub(crate) struct CedarSchema {
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

    use crate::common::policy_store::legacy_store::LegacyAgamaPolicyStore;

    #[test]
    fn test_read_ok() {
        static POLICY_STORE_RAW: &str = include_str!("../../../../test_files/policy-store_ok.yaml");
        serde_yaml_ng::from_str::<LegacyAgamaPolicyStore>(POLICY_STORE_RAW)
            .expect("failed to deserialize policy-store_ok.yaml into LegacyAgamaPolicyStore");
    }

    #[test]
    fn test_agama_ok() {
        static POLICY_STORE_RAW: &str = include_str!("../../../../test_files/agama-store.yaml");
        serde_yaml_ng::from_str::<LegacyAgamaPolicyStore>(POLICY_STORE_RAW)
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
        serde_yaml_ng::from_str::<LegacyAgamaPolicyStore>(YAML_POLICY_STORE)
            .expect("failed to deserialize policy-store_readable.yaml into LegacyAgamaPolicyStore");
    }

    #[test]
    fn test_readable_yaml_identical_readable_json() {
        static YAML_POLICY_STORE: &str =
            include_str!("../../../../test_files/policy-store_readable.yaml");
        static JSON_POLICY_STORE: &str =
            include_str!("../../../../test_files/policy-store_readable.json");

        let yaml_policy_result =
            serde_yaml_ng::from_str::<LegacyAgamaPolicyStore>(YAML_POLICY_STORE);
        let json_policy_result = serde_json::from_str::<LegacyAgamaPolicyStore>(JSON_POLICY_STORE);

        let yaml = yaml_policy_result
            .expect("failed to parse YAML into LegacyAgamaPolicyStore from YAML_POLICY_STORE");
        let json = json_policy_result
            .expect("failed to parse JSON into LegacyAgamaPolicyStore from JSON_POLICY_STORE");
        assert_eq!(
            yaml, json,
            "Parsed LegacyAgamaPolicyStore from YAML and JSON should be equal"
        );
    }
}
