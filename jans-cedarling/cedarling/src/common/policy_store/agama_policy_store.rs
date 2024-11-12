use super::{
    super::cedar_schema::CedarSchemaJson, parse_maybe_cedar_version, parse_option_string,
    trusted_issuer_metadata::TrustedIssuerMetadata, CedarSchema, PolicyStore, TokenKind,
    TrustedIssuer,
};
use base64::prelude::*;
use cedar_policy::{Policy, PolicyId, PolicySet, Schema};
use semver::Version;
use serde::{de, Deserialize};
use std::collections::HashMap;

// Policy Stores from the Agama Policy Designer
#[derive(Debug, PartialEq)]
#[allow(dead_code)]
pub struct AgamaPolicyStore {
    pub name: String,
    pub description: Option<String>,
    pub cedar_version: Option<Version>,
    pub policies: HashMap<String, AgamaPolicyContent>,
    pub cedar_schema: CedarSchema,
    pub trusted_issuers: HashMap<String, TrustedIssuerMetadata>,
}

// Policy Store from the Agama Policy Designer
#[derive(Debug, PartialEq)]
pub struct AgamaPolicyContent {
    pub description: String,
    pub creation_date: String,
    pub policy_content: Policy,
}

#[derive(Deserialize)]
struct RawAgamaPolicyStores {
    #[serde(deserialize_with = "parse_maybe_cedar_version")]
    cedar_version: Option<Version>,
    policy_stores: HashMap<String, RawAgamaPolicyStore>,
}

#[derive(Deserialize)]
struct RawAgamaPolicyStore {
    pub name: String,
    #[serde(deserialize_with = "parse_option_string")]
    pub description: Option<String>,
    policies: HashMap<String, RawAgamaCedarPolicy>,
    /// Base64 encoded JSON Cedar Schema
    #[serde(rename = "schema")]
    encoded_schema: String,
    trusted_issuers: HashMap<String, TrustedIssuerMetadata>,
}

#[derive(Deserialize)]
struct RawAgamaCedarPolicy {
    description: String,
    creation_date: String,
    /// Base64 encoded JSON Cedar Policy
    #[serde(rename = "policy_content")]
    encoded_policy: String,
}

fn decode_b64_string<'de, D>(value: String) -> Result<String, D::Error>
where
    D: de::Deserializer<'de>,
{
    let buf = BASE64_STANDARD
        .decode(value)
        .map_err(|e| de::Error::custom(format!("Failed to decode Base64 encoded string: {}", e)))?;
    String::from_utf8(buf)
        .map_err(|e| de::Error::custom(format!("Failed to decode Base64 encoded string: {}", e)))
}

impl<'de> Deserialize<'de> for AgamaPolicyStore {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: de::Deserializer<'de>,
    {
        let raw_policy_stores = RawAgamaPolicyStores::deserialize(deserializer)?;
        let mut policies = HashMap::new();

        // We use a loop here to get the first item in the HashMap.
        for (_policy_store_id, policy_store) in raw_policy_stores.policy_stores {
            for (policy_id, policy) in policy_store.policies {
                // parse the decoded policy string into a cedar_policy::Policy struct
                let decoded_policy = decode_b64_string::<D>(policy.encoded_policy)?;
                let cedar_policy = Policy::parse(Some(PolicyId::new(&policy_id)), decoded_policy)
                    .map_err(de::Error::custom)?;

                let agama_policy = AgamaPolicyContent {
                    description: policy.description,
                    creation_date: policy.creation_date,
                    policy_content: cedar_policy,
                };

                policies.insert(policy_id, agama_policy);
            }

            let name = policy_store.name;

            // Deserialize Base64 encoded schema into CedarSchema
            let decoded_schema = decode_b64_string::<D>(policy_store.encoded_schema)?;
            let schema = Schema::from_json_str(&decoded_schema).map_err(de::Error::custom)?;
            let json = serde_json::from_str::<CedarSchemaJson>(&decoded_schema)
                .map_err(de::Error::custom)?;
            let cedar_schema = CedarSchema { schema, json };

            // We return early since should only be getting one policy
            // store from Agama and Cedarling only supports using
            // one policy store at a time.
            return Ok(AgamaPolicyStore {
                name,
                description: policy_store.description,
                cedar_version: raw_policy_stores.cedar_version,
                policies,
                cedar_schema,
                trusted_issuers: policy_store.trusted_issuers,
            });
        }

        return Err(de::Error::custom(
            "Failed to deserialize Agama Policy Store: No policy store found in the `policies` field.",
        ));
    }
}

impl From<AgamaPolicyStore> for PolicyStore {
    fn from(agama_store: AgamaPolicyStore) -> Self {
        let mut policy_set = PolicySet::new();
        for (_id, policy) in agama_store.policies {
            policy_set
                .add(policy.policy_content)
                .expect("A non-template linked policy should be used");
        }

        let mut trusted_issuers = Vec::new();
        // we lose the issuer id in this operation so we probably
        // need to update the main policy store as well so wen can log that
        for (_iss_id, iss_metadata) in agama_store.trusted_issuers {
            let mut token_metadata = HashMap::new();
            token_metadata.insert(TokenKind::Access, iss_metadata.access_tokens);
            token_metadata.insert(TokenKind::Id, iss_metadata.id_tokens);
            token_metadata.insert(TokenKind::Userinfo, iss_metadata.userinfo_tokens);
            token_metadata.insert(TokenKind::Transaction, iss_metadata.tx_tokens);

            trusted_issuers.push(TrustedIssuer {
                name: iss_metadata.name,
                description: iss_metadata.description,
                openid_configuration_endpoint: iss_metadata.openid_configuration_endpoint,
                token_metadata: Some(token_metadata),
            });
        }

        PolicyStore {
            name: Some(agama_store.name),
            description: agama_store.description,
            cedar_version: agama_store.cedar_version,
            cedar_schema: agama_store.cedar_schema,
            cedar_policies: policy_set,
            trusted_issuers: Some(trusted_issuers),
        }
    }
}

#[cfg(test)]
mod test {
    use super::super::super::{cedar_schema::CedarSchemaJson, policy_store::CedarSchema};
    use super::{AgamaPolicyContent, AgamaPolicyStore};
    use cedar_policy::{Policy, PolicyId, Schema};
    use semver::Version;
    use std::collections::HashMap;

    #[test]
    fn can_parse_agama_policy_store() {
        let policy_store_json = include_str!("./test_agama_policy_store.json");

        let parsed = serde_json::from_str::<AgamaPolicyStore>(policy_store_json)
            .expect("should parse Agama Policy Store");

        // Create Expected policies
        let mut policies = HashMap::new();
        let policy_id = "fbd921a51b8b78b3b8af5f93e94fbdc57f3e2238b29f".to_string();
        policies.insert(
            policy_id.clone(),
            AgamaPolicyContent {
                description: "Admin".to_string(),
                creation_date: "2024-11-07T07:49:11.813002".to_string(),
                policy_content: Policy::parse(
                    Some(PolicyId::new(policy_id)),
                    r#"@id("Admin")
permit
(
 principal == somecompany::store::Role::"Admin",
 action in [somecompany::store::Action::"DELETE",somecompany::store::Action::"GET",somecompany::store::Action::"PUT"],
 resource == somecompany::store::HTTP_Request::"root"
)
;"#.to_string(),
                )
                .expect("should parse cedar policy"),
            },
        );
        let policy_id = "1a2dd16865cf220ea9807608c6648a457bdf4057c4a4".to_string();
        policies.insert(
            policy_id.clone(),
            AgamaPolicyContent {
                description: "Member".to_string(),
                creation_date: "2024-11-07T07:50:05.520757".to_string(),
                policy_content: Policy::parse(
                    Some(PolicyId::new(policy_id)),
                    r#"@id("Member")
permit
(
 principal == somecompany::store::Role::"Member",
 action in [somecompany::store::Action::"PUT"],
 resource == somecompany::store::HTTP_Request::"root"
)
;"#
                    .to_string(),
                )
                .expect("should parse cedar policy"),
            },
        );

        let schema_json = include_str!("./test_agama_cedar_schema.json");
        let schema =
            Schema::from_json_str(schema_json).expect("Should parse Cedar schema from JSON");
        let json = serde_json::from_str::<CedarSchemaJson>(schema_json)
            .expect("Should parse cedar schema JSON");
        let cedar_schema = CedarSchema { schema, json };

        // No need to test parsing trusted_issuers here since we
        // already have tests in trusted_issuer_metadata.rs
        let trusted_issuers = HashMap::new();

        let expected = AgamaPolicyStore {
            name: "jans::store".to_string(),
            description: None,
            cedar_version: Some(Version::new(4, 0, 0)),
            policies,
            cedar_schema,
            trusted_issuers,
        };

        assert_eq!(
            parsed, expected,
            "Expected to parse AgamaPolicyStore from YAML correctly."
        );
    }
}
