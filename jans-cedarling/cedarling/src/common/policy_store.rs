// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

#[cfg(test)]
mod archive_security_tests;
pub(crate) mod custom_issuer_metadata;
pub(crate) mod log_entry;
#[cfg(test)]
pub(crate) mod test_utils;
pub(crate) mod token_entity_metadata;
pub(crate) mod token_types;

use crate::common::{default_entities::DefaultEntitiesWithWarns, issuer_utils::IssClaim};

pub(crate) mod archive_handler;
pub(crate) mod entity_parser;
pub(crate) mod errors;
pub(crate) mod formats;
pub(crate) mod legacy_store;
pub(crate) mod loader;
pub(crate) mod manager;
pub(crate) mod metadata;
pub(crate) mod migration;
pub(crate) mod policy_parser;
pub(crate) mod schema_parser;
#[cfg(feature = "tools")]
pub(crate) mod validate;
pub(crate) mod validator;
pub(crate) mod vfs_adapter;

use super::cedar_schema::CedarSchema;
use cedar_policy::{ActionConstraint, Effect, EntityTypeName, EntityUid, Policy, PolicyId};
use serde::Serialize;
use std::collections::{HashMap, HashSet};
use url::Url;

#[cfg(test)]
pub(crate) use custom_issuer_metadata::CustomIssuerMetadata;
pub(crate) use custom_issuer_metadata::CustomTokenMetadata;
pub(crate) use token_entity_metadata::TokenEntityMetadata;
pub(crate) use token_types::{TokenTypeKind, TokenTypes, TokenTypesError};

// Re-export types used by init/policy_store.rs and external consumers
pub(crate) use manager::ConversionError;
pub(crate) use metadata::PolicyStoreMetadata;

/// Represents the store of policies used for JWT validation and policy evaluation in Cedarling.
///
/// The `PolicyStore` contains the schema and a set of policies encoded in base64,
/// which are parsed during deserialization.
#[derive(Debug, Clone)]
#[cfg_attr(test, derive(PartialEq))]
pub(crate) struct PolicyStore {
    /// version of policy store
    //
    // alias to support Agama lab format
    pub(crate) version: Option<String>,

    /// Cedar schema (optional — `None` when strict schema validation is disabled)
    pub(crate) schema: Option<CedarSchema>,

    /// Whether a schema source was present in the policy store source,
    /// regardless of whether it was loaded (used for log messages).
    pub(crate) schema_source_exists: bool,

    /// Cedar policy set
    pub(crate) policies: PoliciesContainer,

    /// Every accepted token type, JWT and custom, keyed by the entity type a
    /// request `mapping` names.
    pub(crate) token_types: TokenTypes,

    /// Default entities for the policy store.
    pub(crate) default_entities: DefaultEntitiesWithWarns,
}

impl PolicyStore {
    pub(crate) fn get_store_version(&self) -> &str {
        self.version.as_deref().unwrap_or("undefined")
    }
}

/// Wrapper around [`PolicyStore`] to have access to it and ID of policy store.
///
/// When loaded from the new directory/archive format, includes optional metadata
/// containing version, description, and other policy store information.
#[derive(Clone, derive_more::Deref)]
#[cfg_attr(test, derive(Debug))]
pub(crate) struct PolicyStoreWithID {
    /// ID of policy store
    pub(crate) id: String,
    /// Policy store value
    #[deref]
    pub(crate) store: PolicyStore,
    /// Optional metadata from new format policy stores.
    /// Contains `cedar_version`, `policy_store` info (name, version, description, etc.)
    pub(crate) metadata: Option<metadata::PolicyStoreMetadata>,
    /// `sha256:<hex>` of the raw `.cjar` bytes this store was parsed from; `None`
    /// for stores without an archive (YAML, directory). Lives with the store, so
    /// a refresh swaps the hash together with the policies it identifies.
    /// Private and set only where the archive bytes are read, so a `.cjar` store
    /// cannot exist without its hash.
    bundle_hash: Option<jans_trace_core::Digest>,
}

impl PolicyStoreWithID {
    /// A store that was not read from a `.cjar` (legacy YAML), so it has no `bundle_hash`.
    pub(crate) fn without_archive(
        id: String,
        store: PolicyStore,
        metadata: Option<metadata::PolicyStoreMetadata>,
    ) -> Self {
        Self {
            id,
            store,
            metadata,
            bundle_hash: None,
        }
    }

    /// Hash of the `.cjar` this store came from; `None` without an archive.
    pub(crate) fn bundle_hash(&self) -> Option<jans_trace_core::Digest> {
        self.bundle_hash
    }
}

/// Path suffix that turns an issuer URL into its discovery URL (OIDC Discovery §4).
const DISCOVERY_SUFFIX: &str = "/.well-known/openid-configuration";

/// A configuration endpoint the expected `iss` cannot be derived from.
#[derive(Debug, Clone, PartialEq, thiserror::Error)]
#[error(
    "configuration endpoint '{0}' must be the issuer URL followed by \
     `/.well-known/openid-configuration`, without query or fragment"
)]
pub struct InvalidConfigurationEndpoint(String);

/// Derives the expected `iss` from an OIDC discovery URL, keeping any path
/// before the well-known suffix (OIDC Discovery §4).
pub(crate) fn iss_from_configuration(
    configuration: &Url,
) -> Result<Url, InvalidConfigurationEndpoint> {
    let invalid = || InvalidConfigurationEndpoint(configuration.to_string());
    if configuration.query().is_some() || configuration.fragment().is_some() {
        return Err(invalid());
    }
    let issuer_path = configuration
        .path()
        .strip_suffix(DISCOVERY_SUFFIX)
        .ok_or_else(invalid)?;
    let mut iss = configuration.clone();
    iss.set_path(issuer_path);
    Ok(iss)
}

/// Where an issuer's signing keys come from.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub(crate) enum KeySource {
    /// OIDC discovery; keys come from the discovered `jwks_uri`.
    Discovery { configuration: Url },
    /// Keys fetched straight from a JWKS URL, with no discovery request.
    Static { jwks: Url },
}

/// Represents a trusted issuer that can provide JWTs.
///
/// Holds the `iss` its tokens must carry and where its signing keys come from.
#[derive(Debug, Clone, PartialEq)]
pub(crate) struct TrustedIssuer {
    /// The name of the trusted issuer.
    /// Name also describe namespace in Cedar policy where entity `TrustedIssuer` is located.
    pub(crate) name: String,
    /// The `iss` its tokens must carry.
    // Private to force usage of `iss_claim` method to get normalized iss claim
    iss: Url,
    /// Where the issuer's signing keys come from.
    pub(crate) key_source: KeySource,
    /// Metadata for tokens issued by the trusted issuer, keyed by Cedar entity
    /// type (the request `TokenInput.mapping`).
    pub(crate) token_metadata: HashMap<String, TokenEntityMetadata>,
}

#[cfg(test)]
impl Default for TrustedIssuer {
    fn default() -> Self {
        Self {
            name: "Jans".to_string(),
            // This will only really be called during testing so we just put this test value
            iss: Url::parse("https://test.jans.org").unwrap(),
            key_source: KeySource::Discovery {
                configuration: Url::parse("https://test.jans.org/.well-known/openid-configuration")
                    .unwrap(),
            },
            token_metadata: HashMap::from([
                (
                    "Jans::Access_token".into(),
                    TokenEntityMetadata::access_token(),
                ),
                ("Jans::Id_token".into(), TokenEntityMetadata::id_token()),
                (
                    "Jans::Userinfo_token".into(),
                    TokenEntityMetadata::userinfo_token(),
                ),
            ]),
        }
    }
}

#[cfg(test)]
impl Default for &TrustedIssuer {
    fn default() -> Self {
        static DEFAULT: std::sync::LazyLock<TrustedIssuer> =
            std::sync::LazyLock::new(TrustedIssuer::default);
        &DEFAULT
    }
}

impl TrustedIssuer {
    pub(crate) fn new(
        name: String,
        iss: Url,
        key_source: KeySource,
        metadata: HashMap<String, TokenEntityMetadata>,
    ) -> Self {
        Self {
            name,
            iss,
            key_source,
            token_metadata: metadata,
        }
    }

    /// An issuer loaded through OIDC discovery, with `iss` derived offline
    /// from `configuration`.
    #[cfg(test)]
    pub(crate) fn from_configuration(
        name: String,
        configuration: Url,
        metadata: HashMap<String, TokenEntityMetadata>,
    ) -> Result<Self, InvalidConfigurationEndpoint> {
        let iss = iss_from_configuration(&configuration)?;
        Ok(Self::new(
            name,
            iss,
            KeySource::Discovery { configuration },
            metadata,
        ))
    }

    /// A discovery issuer whose configuration endpoint is `iss` plus the
    /// well-known suffix.
    #[cfg(test)]
    pub(crate) fn discovery_at(
        name: String,
        iss: Url,
        metadata: HashMap<String, TokenEntityMetadata>,
    ) -> Self {
        let configuration = Url::parse(&format!(
            "{}{DISCOVERY_SUFFIX}",
            iss.as_str().trim_end_matches('/')
        ))
        .expect("test configuration endpoint should be a valid url");
        Self::new(name, iss, KeySource::Discovery { configuration }, metadata)
    }

    /// Points the issuer at another discovery endpoint, re-deriving `iss`.
    #[cfg(test)]
    pub(crate) fn set_configuration_endpoint(&mut self, configuration: Url) {
        self.iss = iss_from_configuration(&configuration)
            .expect("test configuration endpoint should derive an iss");
        self.key_source = KeySource::Discovery { configuration };
    }

    /// The `iss` its tokens must carry.
    pub(crate) fn iss(&self) -> &Url {
        &self.iss
    }

    pub(crate) fn iss_claim(&self) -> IssClaim {
        IssClaim::new(self.iss.as_str())
    }
}

/// Container for compiled Cedar policies and their descriptions.
#[derive(Debug, Clone)]
pub(crate) struct PoliciesContainer {
    /// Policy descriptions by ID
    descriptions: HashMap<String, String>,
    /// Compiled `cedar_policy` Policy set
    policy_set: cedar_policy::PolicySet,
}

#[cfg(test)]
impl PartialEq for PoliciesContainer {
    fn eq(&self, other: &Self) -> bool {
        use std::collections::BTreeMap;
        let self_policies: BTreeMap<_, _> = self
            .policy_set
            .policies()
            .map(|p| (p.id().clone(), p))
            .collect();
        let other_policies: BTreeMap<_, _> = other
            .policy_set
            .policies()
            .map(|p| (p.id().clone(), p))
            .collect();
        self_policies == other_policies
    }
}

impl PoliciesContainer {
    #[cfg(feature = "tools")]
    pub(crate) fn all_policy_metadata(&self) -> Vec<PolicyMetadata> {
        self.policy_set
            .policies()
            .map(PolicyMetadata::from_policy)
            .collect()
    }

    /// Create a new `PoliciesContainer` from a policy set and description map.
    pub(crate) fn new(
        policy_set: cedar_policy::PolicySet,
        descriptions: HashMap<String, String>,
    ) -> Self {
        Self {
            descriptions,
            policy_set,
        }
    }

    /// Create an empty `PoliciesContainer` with the given policy set.
    pub(crate) fn new_empty(policy_set: cedar_policy::PolicySet) -> Self {
        Self {
            policy_set,
            descriptions: HashMap::new(),
        }
    }

    /// Get [`cedar_policy::PolicySet`]
    pub(crate) fn get_set(&self) -> &cedar_policy::PolicySet {
        &self.policy_set
    }

    /// Get policy description based on id of policy
    pub(crate) fn get_policy_description(&self, id: &str) -> Option<&str> {
        self.descriptions.get(id).map(String::as_str)
    }

    /// Returns metadata for all policies whose scope constraints are compatible
    /// with the given principal types, action UIDs, and resource types.
    ///
    /// Filtering is scope-level only: policies with `when`/`unless` conditions
    /// may still not apply at evaluation time. The returned set is a superset
    /// of truly applicable policies.
    pub(crate) fn get_matching_policies(
        &self,
        principal_entity_type_names: &HashSet<EntityTypeName>,
        action_uids: &HashSet<EntityUid>,
        resource_entity_type_names: &HashSet<EntityTypeName>,
    ) -> Vec<PolicyMetadata> {
        self.policy_set
            .policies()
            .filter(|policy| {
                matches_principal(policy, principal_entity_type_names)
                    && matches_action(policy, action_uids)
                    && matches_resource(policy, resource_entity_type_names)
            })
            .map(PolicyMetadata::from_policy)
            .collect()
    }

    /// Merge the annotations of the given policies into a single map.
    ///
    /// Lossy: if the same annotation key appears on several policies, one value
    /// wins arbitrarily (iteration order is undefined). Use [`Self::annotation_values`]
    /// or [`Self::annotations_by_policy`] when duplicates matter.
    /// IDs not present in the policy set are silently skipped.
    pub(crate) fn annotations_map<'a>(
        &self,
        ids: impl IntoIterator<Item = &'a PolicyId>,
    ) -> HashMap<String, String> {
        ids.into_iter()
            .filter_map(|id| self.policy_set.policy(id))
            .flat_map(Policy::annotations)
            .map(|(k, v)| (k.to_string(), v.to_string()))
            .collect()
    }

    /// Collect every value of the annotation `key` across the given policies.
    ///
    /// Duplicates are preserved; IDs not present in the policy set are silently skipped.
    pub(crate) fn annotation_values<'a>(
        &self,
        ids: impl IntoIterator<Item = &'a PolicyId>,
        key: &str,
    ) -> Vec<String> {
        ids.into_iter()
            .filter_map(|id| self.policy_set.policy(id))
            .flat_map(Policy::annotations)
            .filter(|(k, _)| *k == key)
            .map(|(_, v)| v.to_string())
            .collect()
    }

    /// Return the annotations of each given policy, grouped by policy ID.
    ///
    /// Loss-free companion to [`Self::annotations_map`]. IDs not present in the
    /// policy set are silently skipped.
    pub(crate) fn annotations_by_policy<'a>(
        &self,
        ids: impl IntoIterator<Item = &'a PolicyId>,
    ) -> HashMap<String, HashMap<String, String>> {
        ids.into_iter()
            .filter_map(|id| self.policy_set.policy(id))
            .map(|policy| {
                (
                    policy.id().to_string(),
                    policy
                        .annotations()
                        .map(|(k, v)| (k.to_string(), v.to_string()))
                        .collect(),
                )
            })
            .collect()
    }
}

/// The effect of a Cedar policy: either `Permit` or `Forbid`.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize)]
#[serde(rename_all = "lowercase")]
pub enum PolicyEffect {
    /// The policy permits the request.
    Permit,
    /// The policy forbids the request.
    Forbid,
}

impl From<Effect> for PolicyEffect {
    fn from(effect: Effect) -> Self {
        match effect {
            Effect::Permit => PolicyEffect::Permit,
            Effect::Forbid => PolicyEffect::Forbid,
        }
    }
}

/// Metadata about a single Cedar policy.
#[derive(Debug, Clone, PartialEq, Serialize)]
pub struct PolicyMetadata {
    /// The policy ID
    pub id: String,
    /// The effect of the policy (`Permit` or `Forbid`)
    pub effect: PolicyEffect,
    /// Key-value pairs from Cedar policy annotations (`@key("value")`)
    pub annotations: HashMap<String, String>,
    /// The Cedar policy source code (human-readable Cedar syntax)
    pub source: String,
}

impl PolicyMetadata {
    fn from_policy(policy: &Policy) -> Self {
        Self {
            id: policy.id().to_string(),
            effect: policy.effect().into(),
            annotations: policy
                .annotations()
                .map(|(k, v)| (k.to_string(), v.to_string()))
                .collect(),
            source: policy.to_string(),
        }
    }
}

/// Check if a policy's principal scope constraint matches any of the given principal entity types.
fn matches_principal(policy: &Policy, principal_types: &HashSet<EntityTypeName>) -> bool {
    use cedar_policy::PrincipalConstraint;
    match policy.principal_constraint() {
        PrincipalConstraint::Any => true,
        PrincipalConstraint::Eq(euid) | PrincipalConstraint::In(euid) => {
            principal_types.contains(euid.type_name())
        },
        PrincipalConstraint::Is(type_name) | PrincipalConstraint::IsIn(type_name, _) => {
            principal_types.contains(&type_name)
        },
    }
}

/// Check if a policy's action scope constraint matches any of the given action UIDs.
fn matches_action(policy: &Policy, action_uids: &HashSet<EntityUid>) -> bool {
    match policy.action_constraint() {
        ActionConstraint::Any => true,
        ActionConstraint::Eq(euid) => action_uids.contains(&euid),
        ActionConstraint::In(euids) => euids.iter().any(|euid| action_uids.contains(euid)),
    }
}

/// Check if a policy's resource scope constraint matches any of the given resource entity types.
fn matches_resource(policy: &Policy, resource_types: &HashSet<EntityTypeName>) -> bool {
    use cedar_policy::ResourceConstraint;
    match policy.resource_constraint() {
        ResourceConstraint::Any => true,
        ResourceConstraint::Eq(euid) | ResourceConstraint::In(euid) => {
            resource_types.contains(euid.type_name())
        },
        ResourceConstraint::Is(type_name) | ResourceConstraint::IsIn(type_name, _) => {
            resource_types.contains(&type_name)
        },
    }
}

/// Checks whether byte content looks like a JSON document (object or array),
/// used only to report legacy JSON policy stores with a precise error.
pub(crate) fn is_json_bytes(bytes: &[u8]) -> bool {
    bytes
        .iter()
        .find(|b| !b.is_ascii_whitespace())
        .is_some_and(|b| *b == b'{' || *b == b'[')
}

/// Checks whether text content looks like a JSON document (object or array),
/// used only to report legacy JSON policy stores with a precise error.
#[inline]
pub(crate) fn is_json_content(content: &str) -> bool {
    is_json_bytes(content.as_bytes())
}

#[cfg(test)]
mod json_content_detection_tests {
    use super::{is_json_bytes, is_json_content};

    #[test]
    fn test_is_json_content_detects_json() {
        assert!(
            is_json_content(r#"{"cedar_version": "v4.0.0"}"#),
            "pure JSON object should be detected as JSON"
        );
        assert!(
            is_json_content(r#"[{"id": "1"}]"#),
            "pure JSON array should be detected as JSON"
        );
        assert!(
            is_json_content("  \n\t  {\n  \"key\": \"value\"\n}"),
            "whitespace-prefixed JSON should be detected as JSON"
        );
        assert!(
            is_json_bytes(b"  {\"key\": \"value\"}"),
            "raw bytes should be detected as JSON"
        );
    }

    #[test]
    fn test_is_json_content_allows_valid_yaml() {
        assert!(
            !is_json_content("cedar_version: v4.0.0\npolicies:\n  allow: true"),
            "valid YAML should not be detected as JSON"
        );
        assert!(
            !is_json_content("# leading comment\ncedar_version: v4.0.0"),
            "commented YAML should not be detected as JSON"
        );
        assert!(
            !is_json_content("---\ncedar_version: v4.0.0"),
            "document-marker YAML should not be detected as JSON"
        );
        assert!(
            !is_json_content("cedar_version: v4.0.0\npolicies: { allow: true }"),
            "YAML with nested flow mapping should not be detected as JSON"
        );
        assert!(
            !is_json_content(""),
            "empty string should not be detected as JSON"
        );
        assert!(
            !is_json_content("   \n\t  \n"),
            "whitespace-only string should not be detected as JSON"
        );
        assert!(
            !is_json_bytes(b"\x00\x01\x02"),
            "arbitrary binary should not be detected as JSON"
        );
    }
}

#[cfg(test)]
mod policy_metadata_tests {
    use super::*;
    use cedar_policy::{EntityTypeName, EntityUid, PolicyId, PolicySet};
    use std::collections::HashSet;
    use std::str::FromStr;

    /// Helper: build a `PoliciesContainer` from individual policies with explicit IDs.
    fn make_container(policies: &[(&str, &str)]) -> PoliciesContainer {
        let mut policy_set = PolicySet::new();
        for (id, src) in policies {
            let policy = Policy::parse(Some(PolicyId::new(id)), *src).expect("bad policy");
            policy_set.add(policy).expect("duplicate policy id");
        }
        PoliciesContainer::new_empty(policy_set)
    }

    fn type_names(names: &[&str]) -> HashSet<EntityTypeName> {
        names
            .iter()
            .map(|n| EntityTypeName::from_str(n).unwrap())
            .collect()
    }

    fn action_uids(actions: &[&str]) -> HashSet<EntityUid> {
        actions
            .iter()
            .map(|a| EntityUid::from_str(a).unwrap())
            .collect()
    }

    #[test]
    fn matches_all_policies_with_any_constraints() {
        let container = make_container(&[("open", "permit(principal, action, resource);")]);

        let result = container.get_matching_policies(
            &type_names(&["Foo::Bar"]),
            &action_uids(&[r#"Foo::Action::"read""#]),
            &type_names(&["Foo::Resource"]),
        );

        assert_eq!(result.len(), 1);
        assert_eq!(result[0].id, "open");
    }

    #[test]
    fn filters_by_action() {
        let container = make_container(&[
            (
                "read_policy",
                r#"permit(principal, action == Jans::Action::"Read", resource);"#,
            ),
            (
                "write_policy",
                r#"permit(principal, action == Jans::Action::"Write", resource);"#,
            ),
        ]);

        let result = container.get_matching_policies(
            &type_names(&["Jans::User"]),
            &action_uids(&[r#"Jans::Action::"Read""#]),
            &type_names(&["Jans::Document"]),
        );

        assert_eq!(result.len(), 1);
        assert_eq!(result[0].id, "read_policy");
    }

    #[test]
    fn filters_by_principal_type() {
        let container = make_container(&[
            (
                "workload_policy",
                "permit(principal is Jans::Workload, action, resource);",
            ),
            (
                "user_policy",
                "permit(principal is Jans::User, action, resource);",
            ),
        ]);

        let result = container.get_matching_policies(
            &type_names(&["Jans::User"]),
            &action_uids(&[r#"Jans::Action::"Read""#]),
            &type_names(&["Jans::Doc"]),
        );

        assert_eq!(result.len(), 1);
        assert_eq!(result[0].id, "user_policy");
    }

    #[test]
    fn filters_by_resource_type() {
        let container = make_container(&[
            (
                "issue_policy",
                "permit(principal, action, resource is Jans::Issue);",
            ),
            (
                "doc_policy",
                "permit(principal, action, resource is Jans::Document);",
            ),
        ]);

        let result = container.get_matching_policies(
            &type_names(&["Jans::User"]),
            &action_uids(&[r#"Jans::Action::"Read""#]),
            &type_names(&["Jans::Issue"]),
        );

        assert_eq!(result.len(), 1);
        assert_eq!(result[0].id, "issue_policy");
    }

    #[test]
    fn combined_filtering() {
        let container = make_container(&[
            (
                "specific",
                r#"permit(
                    principal is Jans::User,
                    action == Jans::Action::"Read",
                    resource is Jans::Document
                );"#,
            ),
            (
                "other_action",
                r#"permit(
                    principal is Jans::User,
                    action == Jans::Action::"Delete",
                    resource is Jans::Document
                );"#,
            ),
            (
                "other_principal",
                r#"permit(
                    principal is Jans::Workload,
                    action == Jans::Action::"Read",
                    resource is Jans::Document
                );"#,
            ),
        ]);

        let result = container.get_matching_policies(
            &type_names(&["Jans::User"]),
            &action_uids(&[r#"Jans::Action::"Read""#]),
            &type_names(&["Jans::Document"]),
        );

        assert_eq!(result.len(), 1);
        assert_eq!(result[0].id, "specific");
    }

    #[test]
    fn action_in_constraint() {
        let container = make_container(&[(
            "multi_action",
            r#"permit(
                principal,
                action in [Jans::Action::"Read", Jans::Action::"List"],
                resource
            );"#,
        )]);

        let result = container.get_matching_policies(
            &type_names(&["Jans::User"]),
            &action_uids(&[r#"Jans::Action::"Read""#]),
            &type_names(&["Jans::Doc"]),
        );
        assert_eq!(result.len(), 1);

        let result = container.get_matching_policies(
            &type_names(&["Jans::User"]),
            &action_uids(&[r#"Jans::Action::"Delete""#]),
            &type_names(&["Jans::Doc"]),
        );
        assert_eq!(result.len(), 0);
    }

    #[test]
    fn annotations_are_extracted() {
        let mut policy_set = PolicySet::new();
        let policy = Policy::parse(
            Some(PolicyId::new("annotated")),
            r#"
            @description("Allow users to read docs")
            @category("access_control")
            permit(principal, action, resource);
            "#,
        )
        .unwrap();
        policy_set.add(policy).unwrap();
        let container = PoliciesContainer::new_empty(policy_set);

        let result = container.get_matching_policies(
            &type_names(&["Jans::User"]),
            &action_uids(&[r#"Jans::Action::"Read""#]),
            &type_names(&["Jans::Doc"]),
        );

        assert_eq!(result.len(), 1);
        let policy = &result[0];
        assert_eq!(
            policy.annotations.get("description").unwrap(),
            "Allow users to read docs"
        );
        assert_eq!(
            policy.annotations.get("category").unwrap(),
            "access_control"
        );
    }

    #[test]
    fn source_is_recoverable() {
        let container = make_container(&[("p", "permit(principal, action, resource);")]);

        let result = container.get_matching_policies(
            &type_names(&["Any::Type"]),
            &action_uids(&[r#"Any::Action::"do""#]),
            &type_names(&["Any::Res"]),
        );

        assert_eq!(result.len(), 1);
        assert!(result[0].source.contains("permit"));
    }

    #[test]
    fn empty_inputs_return_only_any_policies() {
        let container = make_container(&[
            ("any_policy", "permit(principal, action, resource);"),
            (
                "specific_policy",
                r#"permit(
                    principal is Jans::User,
                    action == Jans::Action::"Read",
                    resource is Jans::Doc
                );"#,
            ),
        ]);

        let result =
            container.get_matching_policies(&HashSet::new(), &HashSet::new(), &HashSet::new());

        // Only the "any" policy matches — specific constraints can't match empty sets
        assert_eq!(result.len(), 1);
        assert_eq!(result[0].id, "any_policy");
    }

    #[test]
    fn multiple_principals_match_union() {
        let container = make_container(&[
            (
                "workload_policy",
                "permit(principal is Jans::Workload, action, resource);",
            ),
            (
                "user_policy",
                "permit(principal is Jans::User, action, resource);",
            ),
        ]);

        let result = container.get_matching_policies(
            &type_names(&["Jans::User", "Jans::Workload"]),
            &action_uids(&[r#"Jans::Action::"Read""#]),
            &type_names(&["Jans::Doc"]),
        );

        assert_eq!(result.len(), 2);
    }

    fn annotated_container() -> PoliciesContainer {
        make_container(&[
            (
                "upgrade",
                r#"
                @redirect("/upgrade")
                @tier("premium")
                permit(principal, action, resource);
                "#,
            ),
            (
                "trial",
                r#"
                @redirect("/trial")
                @audit("true")
                permit(principal, action, resource);
                "#,
            ),
        ])
    }

    #[test]
    fn annotations_map_merges_policies() {
        let container = annotated_container();
        let ids = [PolicyId::new("upgrade"), PolicyId::new("trial")];

        let result = container.annotations_map(ids.iter());

        // "redirect" collides across policies: one value survives, order undefined
        assert_eq!(result.len(), 3);
        assert!(["/upgrade", "/trial"].contains(&result["redirect"].as_str()));
        assert_eq!(result["tier"], "premium");
        assert_eq!(result["audit"], "true");
    }

    #[test]
    fn annotations_map_skips_missing_ids() {
        let container = annotated_container();
        let ids = [PolicyId::new("upgrade"), PolicyId::new("no_such_policy")];

        let result = container.annotations_map(ids.iter());

        assert_eq!(result.len(), 2);
        assert_eq!(result["redirect"], "/upgrade");
        assert_eq!(result["tier"], "premium");
    }

    #[test]
    fn annotation_values_preserves_duplicates() {
        let container = annotated_container();
        let ids = [PolicyId::new("upgrade"), PolicyId::new("trial")];

        let mut result = container.annotation_values(ids.iter(), "redirect");
        result.sort();
        assert_eq!(result, ["/trial", "/upgrade"]);

        assert_eq!(container.annotation_values(ids.iter(), "audit"), ["true"]);
        assert_eq!(
            container.annotation_values(ids.iter(), "absent"),
            [] as [std::string::String; 0]
        );
    }

    #[test]
    fn annotations_by_policy_groups_per_policy() {
        let container = annotated_container();
        let ids = [
            PolicyId::new("upgrade"),
            PolicyId::new("trial"),
            PolicyId::new("no_such_policy"),
        ];

        let result = container.annotations_by_policy(ids.iter());

        assert_eq!(result.len(), 2);
        assert_eq!(result["upgrade"]["redirect"], "/upgrade");
        assert_eq!(result["upgrade"]["tier"], "premium");
        assert_eq!(result["trial"]["redirect"], "/trial");
        assert_eq!(result["trial"]["audit"], "true");
    }

    #[test]
    fn annotations_of_unannotated_policy_are_empty() {
        let container = make_container(&[("plain", "permit(principal, action, resource);")]);
        let ids = [PolicyId::new("plain")];

        assert!(container.annotations_map(ids.iter()).is_empty());
        assert_eq!(
            container.annotation_values(ids.iter(), "any"),
            [] as [std::string::String; 0]
        );

        let by_policy = container.annotations_by_policy(ids.iter());
        assert_eq!(by_policy.len(), 1);
        assert!(by_policy["plain"].is_empty());
    }
}

#[cfg(test)]
mod iss_from_configuration_tests {
    use super::*;

    fn derive(configuration: &str) -> Result<Url, InvalidConfigurationEndpoint> {
        iss_from_configuration(&Url::parse(configuration).expect("test url should parse"))
    }

    #[test]
    fn root_issuer_keeps_origin() {
        let iss = derive("https://idp.example/.well-known/openid-configuration")
            .expect("a root discovery URL should derive an iss");
        assert_eq!(
            IssClaim::new(iss.as_str()),
            IssClaim::new("https://idp.example"),
            "a root issuer should derive its origin"
        );
    }

    #[test]
    fn path_before_suffix_is_kept() {
        for (configuration, expected) in [
            (
                "https://kc.example/realms/a/.well-known/openid-configuration",
                "https://kc.example/realms/a",
            ),
            (
                "https://login.microsoftonline.com/tenant-1/v2.0/.well-known/openid-configuration",
                "https://login.microsoftonline.com/tenant-1/v2.0",
            ),
        ] {
            let iss = derive(configuration).expect("a path-based discovery URL should derive");
            assert_eq!(
                iss.as_str(),
                expected,
                "{configuration} should keep the issuer path"
            );
        }
    }

    #[test]
    fn rejects_url_not_of_discovery_shape() {
        for configuration in [
            "https://idp.example",
            "https://idp.example/auth",
            "https://idp.example/.well-known/openid-configuration/extra",
            "https://idp.example/.well-known/openid-configuration?tenant=a",
            "https://idp.example/.well-known/openid-configuration#frag",
        ] {
            let err = derive(configuration)
                .expect_err("a URL not of the discovery shape must be rejected");
            assert_eq!(
                err,
                InvalidConfigurationEndpoint(
                    Url::parse(configuration).expect("valid url").to_string()
                ),
                "the error should name {configuration}"
            );
        }
    }
}
