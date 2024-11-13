use super::{
    super::{
        super::cedar_schema::CedarSchemaJson, parse_maybe_cedar_version, parse_option_string,
        trusted_issuer_metadata::TrustedIssuerMetadata, CedarSchema,
    },
    PolicyContent, PolicyStoreSource,
};
use cedar_policy::{
    ffi::{self},
    Policy, PolicyId, Schema,
};
use semver::Version;
use serde::{de, Deserialize, Deserializer};
use std::{collections::HashMap, str::FromStr};

/// Policy Store used for internal testing which supports readable
/// Cedar policies and schema.
#[derive(Deserialize)]
pub struct PolicyStoreSourceYaml {
    #[serde(default)]
    pub name: String,
    #[serde(deserialize_with = "parse_option_string", default)]
    pub description: Option<String>,
    #[serde(deserialize_with = "parse_maybe_cedar_version", default)]
    pub cedar_version: Option<Version>,
    #[serde(deserialize_with = "parse_human_readable_policies", default)]
    pub policies: HashMap<String, PolicyContent>,
    #[serde(alias = "schema", deserialize_with = "parse_human_readable_schema")]
    pub cedar_schema: CedarSchema,
    #[serde(default)]
    pub trusted_issuers: HashMap<String, TrustedIssuerMetadata>,
}

// Parse a Hashmap of Human-redable Cedar policies
fn parse_human_readable_policies<'de, D>(
    deserializer: D,
) -> Result<HashMap<String, PolicyContent>, D::Error>
where
    D: Deserializer<'de>,
{
    #[derive(Deserialize)]
    struct RawPolicy {
        description: String,
        creation_date: String,
        #[serde(rename = "policy_content")]
        cedar_policy: String,
    }

    let raw_policies = <HashMap<String, RawPolicy> as de::Deserialize>::deserialize(deserializer)?;
    let mut policies = HashMap::new();

    for (policy_id, policy) in raw_policies {
        // parse the decoded policy string into a cedar_policy::Policy struct
        let cedar_policy = Policy::parse(Some(PolicyId::new(&policy_id)), policy.cedar_policy)
            .map_err(de::Error::custom)?;

        let agama_policy = PolicyContent {
            description: policy.description,
            creation_date: policy.creation_date,
            policy_content: cedar_policy,
        };

        policies.insert(policy_id, agama_policy);
    }

    Ok(policies)
}

// Parse a human-readable Cedar schema
fn parse_human_readable_schema<'de, D>(deserializer: D) -> Result<CedarSchema, D::Error>
where
    D: Deserializer<'de>,
{
    let cedar_schema = String::deserialize(deserializer)?;
    let schema = Schema::from_str(&cedar_schema)
        .map_err(|e| de::Error::custom(format!("Failed to load Schema from JSON: {:?}", e)))?;

    // we convert the Schema to a JSON format because it's needed for the
    // rest of the code.
    let ffi_schema = ffi::Schema::Cedar(cedar_schema.clone());
    let ffi_json_schema = match ffi::schema_to_json(ffi_schema) {
        ffi::SchemaToJsonAnswer::Success { json, .. } => json,
        ffi::SchemaToJsonAnswer::Failure { errors } => {
            return Err(de::Error::custom(format!(
                "Error converting Cedar schema to JSON: {:?}",
                errors
            )))
        },
    };
    let json = serde_json::to_string(&ffi_json_schema).unwrap();
    // println!("\n{}\n", json);
    let json = serde_json::from_str::<CedarSchemaJson>(&json).map_err(|e| {
        de::Error::custom(format!("Failed to Load CedarSchemaJson from JSON: {:?}", e))
    })?;

    Ok(CedarSchema { schema, json })
}

impl From<PolicyStoreSourceYaml> for PolicyStoreSource {
    fn from(yaml_store: PolicyStoreSourceYaml) -> Self {
        Self {
            name: yaml_store.name,
            description: yaml_store.description,
            cedar_version: yaml_store.cedar_version,
            policies: yaml_store.policies,
            cedar_schema: yaml_store.cedar_schema,
            trusted_issuers: yaml_store.trusted_issuers,
        }
    }
}

#[derive(Debug, thiserror::Error)]
pub enum LoadFromYamlError {
    #[error("Failed to load policy store from YAML: {0}")]
    Deserialization(serde_yml::Error),
}

#[cfg(test)]
mod test {
    use super::super::super::super::{cedar_schema::CedarSchemaJson, policy_store::CedarSchema};
    use super::super::PolicyContent;
    use super::{parse_human_readable_policies, parse_human_readable_schema};
    use cedar_policy::{ffi, Policy, PolicyId, Schema};
    use serde::Deserialize;
    use std::collections::HashMap;
    use std::str::FromStr;
    use test_utils::assert_eq;

    fn load_from_cedar_schema_from_str(cedar_schema: &str) -> (CedarSchema, String) {
        let schema = Schema::from_str(&cedar_schema).expect("Should load Schema from str.");

        let ffi_schema = ffi::Schema::Cedar(cedar_schema.to_string());
        let ffi_json_schema = match ffi::schema_to_json(ffi_schema) {
            ffi::SchemaToJsonAnswer::Success { json, .. } => json,
            ffi::SchemaToJsonAnswer::Failure { .. } => {
                panic!("Should convert schema from Cedar format to JSON")
            },
        };
        let json_schema = serde_json::to_string(&ffi_json_schema)
            .expect("Should serialize schema to JSON string");
        let json = serde_json::from_str::<CedarSchemaJson>(&json_schema)
            .expect("Should deserialize JSON schema to CedarSchemaJSON");

        (CedarSchema { schema, json }, json_schema)
    }

    #[test]
    fn can_parse_human_readable_policies() {
        #[derive(Deserialize, Debug, PartialEq)]
        struct TestPolicy {
            #[serde(deserialize_with = "parse_human_readable_policies")]
            pub policies: HashMap<String, PolicyContent>,
        }

        // Create Expected policies
        let mut policies = HashMap::new();
        let policy_id = "some_policy_id".to_string();
        policies.insert(
            policy_id.clone(),
            PolicyContent {
                description: "Admin".to_string(),
                creation_date: "2024-11-07T07:49:11.813002".to_string(),
                policy_content: Policy::parse(
                    Some(PolicyId::new(policy_id)),
                    r#"@id("Admin")
permit (
 principal == somecompany::store::Role::"Admin",
 action in [somecompany::store::Action::"DELETE",somecompany::store::Action::"GET",somecompany::store::Action::"PUT"],
 resource == somecompany::store::HTTP_Request::"root"
);"#.to_string(),
                )
                .expect("should parse cedar policy"),
            },
        );
        let policy_id = "another_policy_id".to_string();
        policies.insert(
            policy_id.clone(),
            PolicyContent {
                description: "Member".to_string(),
                creation_date: "2024-11-07T07:50:05.520757".to_string(),
                policy_content: Policy::parse(
                    Some(PolicyId::new(policy_id)),
                    r#"@id("Member")
permit (
 principal == somecompany::store::Role::"Member",
 action in [somecompany::store::Action::"PUT"],
 resource == somecompany::store::HTTP_Request::"root"
);"#
                    .to_string(),
                )
                .expect("should parse cedar policy"),
            },
        );
        let expected = TestPolicy { policies };

        let yaml_policies = r#"policies:
          some_policy_id:
            description: Admin
            creation_date: 2024-11-07T07:49:11.813002
            policy_content: |-
              @id("Admin")
              permit (
               principal == somecompany::store::Role::"Admin",
               action in [somecompany::store::Action::"DELETE",somecompany::store::Action::"GET",somecompany::store::Action::"PUT"],
               resource == somecompany::store::HTTP_Request::"root"
              );
          another_policy_id:
            description: Member
            creation_date: 2024-11-07T07:50:05.520757
            policy_content: |-
              @id("Member")
              permit (
               principal == somecompany::store::Role::"Member",
               action in [somecompany::store::Action::"PUT"],
               resource == somecompany::store::HTTP_Request::"root"
              );"#;

        let parsed_policies = serde_yml::from_str::<TestPolicy>(yaml_policies)
            .expect("Failed to parse policies from YAML");

        assert_eq!(
            parsed_policies, expected,
            "Expected to parse policies correctly"
        );
    }

    #[test]
    fn can_parse_human_readable_schema() {
        #[derive(Debug, PartialEq, Deserialize)]
        struct TestPolicyStore {
            #[serde(alias = "schema", deserialize_with = "parse_human_readable_schema")]
            pub cedar_schema: CedarSchema,
        }

        let cedar_schema_str = r#"namespace Jans {
    type Url = {"host": String, "path": String, "protocol": String};
    entity Access_token = {"aud": String, "exp": Long, "iat": Long, "iss": TrustedIssuer, "jti": String};
    entity Issue = {"country": String, "org_id": String};
    entity Role;
    entity TrustedIssuer = {"issuer_entity_id": Url};

    entity User in [Role] = {"country": String, "email": String, "sub": String, "username": String};
    entity Workload = {"client_id": String, "iss": TrustedIssuer, "name": String, "org_id": String};

    entity id_token = {"acr": String, "amr": String, "aud": String, "exp": Long, "iat": Long, "iss": TrustedIssuer, "jti": String, "sub": String};
    action "Update" appliesTo {

        principal: [Workload, User, Role],
        resource: [Issue],

        context: {}
    };
}"#;

        let (cedar_schema, _) = load_from_cedar_schema_from_str(cedar_schema_str);
        let expected = TestPolicyStore { cedar_schema };

        let yaml_schema = r#"schema: |-
    namespace Jans {
        type Url = {"host": String, "path": String, "protocol": String};

        entity Access_token = {"aud": String, "exp": Long, "iat": Long, "iss": TrustedIssuer, "jti": String};
        entity Issue = {"country": String, "org_id": String};
        entity Role;
        entity TrustedIssuer = {"issuer_entity_id": Url};
        entity User in [Role] = {"country": String, "email": String, "sub": String, "username": String};
        entity Workload = {"client_id": String, "iss": TrustedIssuer, "name": String, "org_id": String};
        entity id_token = {"acr": String, "amr": String, "aud": String, "exp": Long, "iat": Long, "iss": TrustedIssuer, "jti": String, "sub": String};
        action "Update" appliesTo {
            principal: [Workload, User, Role],

            resource: [Issue],
            context: {}

        };
    }"#;

        let parsed = serde_yml::from_str::<TestPolicyStore>(yaml_schema)
            .expect("Failed to parse schema from YAML");

        assert_eq!(
            parsed, expected,
            "Expected to parse schema from YAML correctly"
        );
    }
}
