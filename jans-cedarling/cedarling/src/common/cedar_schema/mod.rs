// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

#[cfg(test)]
use std::collections::{BTreeMap, BTreeSet};

#[cfg(test)]
use cedar_policy_core::validator::ValidatorActionId;
use cedar_policy_core::validator::ValidatorSchema;
#[cfg(test)]
use cedar_policy_core::validator::types::{Attributes, Type};

/// The Cedar schema of a policy store.
///
/// Holds only [`cedar_policy::Schema`]: the validator view is derived from it on demand,
/// so the two can never disagree.
#[derive(Debug, Clone)]
pub(crate) struct CedarSchema {
    pub schema: cedar_policy::Schema,
}

impl CedarSchema {
    /// The validator view of the schema, with common types inlined and entity type names
    /// fully qualified.
    pub(crate) fn validator_schema(&self) -> &ValidatorSchema {
        self.schema.as_ref()
    }
}

impl From<cedar_policy::Schema> for CedarSchema {
    fn from(schema: cedar_policy::Schema) -> Self {
        Self { schema }
    }
}

/// Order-independent view of what a schema declares, so tests can compare schemas
/// that were built from different sources (one file or several, Cedar or JSON syntax).
#[cfg(test)]
#[derive(Debug, PartialEq)]
pub(crate) struct SchemaShape {
    entity_types: BTreeMap<String, EntityTypeShape>,
    actions: BTreeMap<String, ActionShape>,
}

#[cfg(test)]
#[derive(Debug, PartialEq)]
struct EntityTypeShape {
    attributes: Attributes,
    tag_type: Option<Type>,
    ancestors: BTreeSet<String>,
}

#[cfg(test)]
#[derive(Debug, PartialEq)]
struct ActionShape {
    context: Option<Type>,
    principals: BTreeSet<String>,
    resources: BTreeSet<String>,
    /// Actions that are members of this action group, directly or transitively; Cedar
    /// builds action ancestry from it.
    descendants: BTreeSet<String>,
}

#[cfg(test)]
impl SchemaShape {
    pub(crate) fn of(schema: &cedar_policy::Schema) -> Self {
        let validator: &ValidatorSchema = schema.as_ref();

        let entity_types = schema
            .entity_types()
            .map(|name| {
                let entity_type = validator.get_entity_type(name.as_ref());
                let shape = EntityTypeShape {
                    attributes: entity_type
                        .map(|et| et.attributes().clone())
                        .unwrap_or_default(),
                    tag_type: entity_type.and_then(|et| et.tag_type().cloned()),
                    ancestors: names(schema.ancestors(name)),
                };
                (name.to_string(), shape)
            })
            .collect();

        let actions = schema
            .actions()
            .map(|action| {
                let shape = ActionShape {
                    context: validator.context_type(action.as_ref()).cloned(),
                    principals: names(schema.principals_for_action(action)),
                    resources: names(schema.resources_for_action(action)),
                    descendants: validator
                        .get_action_id(action.as_ref())
                        .into_iter()
                        .flat_map(ValidatorActionId::descendants)
                        .map(ToString::to_string)
                        .collect(),
                };
                (action.to_string(), shape)
            })
            .collect();

        Self {
            entity_types,
            actions,
        }
    }
}

#[cfg(test)]
fn names<'a>(
    iter: Option<impl Iterator<Item = &'a cedar_policy::EntityTypeName>>,
) -> BTreeSet<String> {
    iter.into_iter()
        .flatten()
        .map(ToString::to_string)
        .collect()
}

#[cfg(test)]
impl PartialEq for CedarSchema {
    fn eq(&self, other: &Self) -> bool {
        SchemaShape::of(&self.schema) == SchemaShape::of(&other.schema)
    }
}

#[cfg(test)]
mod tests {
    use super::SchemaShape;
    use crate::common::policy_store::legacy_store::LegacyAgamaPolicyStore;
    use std::str::FromStr;

    const SCHEMA: &str = r#"
        namespace Jans {
            entity User { name: String };
            entity Issue;
            action "read" appliesTo { principal: [User], resource: [Issue], context: { user: User } };
        }
    "#;

    fn shape(src: &str) -> SchemaShape {
        SchemaShape::of(&cedar_policy::Schema::from_str(src).expect("test schema is valid"))
    }

    #[test]
    fn schema_shape_is_equal_for_cedar_and_json_syntax() {
        let (fragment, _) =
            cedar_policy::SchemaFragment::from_cedarschema_str(SCHEMA).expect("test schema parses");
        let json = fragment.to_json_string().expect("test schema serializes");
        let from_json = cedar_policy::Schema::from_json_str(&json).expect("JSON schema is valid");

        assert_eq!(
            shape(SCHEMA),
            SchemaShape::of(&from_json),
            "the same schema in Cedar and JSON syntax should compare equal"
        );
    }

    #[test]
    fn schema_shape_differs_when_declarations_differ() {
        let changes = [
            (
                "attribute type",
                SCHEMA.replace("name: String", "name: Long"),
            ),
            (
                "context",
                SCHEMA.replace("context: { user: User }", "context: {}"),
            ),
            (
                "applies to",
                SCHEMA.replace("resource: [Issue]", "resource: [User]"),
            ),
            (
                "entity type",
                SCHEMA.replace("entity Issue;", "entity Issue; entity Extra;"),
            ),
        ];
        for (label, changed) in changes {
            assert_ne!(
                shape(SCHEMA),
                shape(&changed),
                "a different {label} should make schemas compare unequal"
            );
        }
    }

    const GROUPED_SCHEMA: &str = r#"
        namespace Jans {
            entity User;
            action "view";
            action "edit";
            action "read" in ["view", "edit"] appliesTo { principal: [User], resource: [User] };
        }
    "#;

    #[test]
    fn schema_shape_ignores_action_membership_order() {
        assert_eq!(
            shape(GROUPED_SCHEMA),
            shape(&GROUPED_SCHEMA.replace(r#"["view", "edit"]"#, r#"["edit", "view"]"#)),
            "the order of action groups in `in [...]` should not affect comparison"
        );
    }

    #[test]
    fn schema_shape_differs_when_action_membership_differs() {
        assert_ne!(
            shape(GROUPED_SCHEMA),
            shape(&GROUPED_SCHEMA.replace(r#"["view", "edit"]"#, r#"["view"]"#)),
            "removing an action from an action group should make schemas compare unequal"
        );
    }

    #[test]
    fn test_read_ok() {
        static POLICY_STORE_RAW: &str = include_str!("../../../../test_files/policy-store_ok.yaml");
        serde_saphyr::from_str::<LegacyAgamaPolicyStore>(POLICY_STORE_RAW)
            .expect("failed to deserialize policy-store_ok.yaml into LegacyAgamaPolicyStore");
    }

    #[test]
    fn test_agama_ok() {
        static POLICY_STORE_RAW: &str = include_str!("../../../../test_files/agama-store.yaml");
        serde_saphyr::from_str::<LegacyAgamaPolicyStore>(POLICY_STORE_RAW)
            .expect("failed to deserialize agama-store.yaml into LegacyAgamaPolicyStore");
    }

    #[test]
    fn test_readable_yaml_ok() {
        static YAML_POLICY_STORE: &str =
            include_str!("../../../../test_files/policy-store_readable.yaml");
        serde_saphyr::from_str::<LegacyAgamaPolicyStore>(YAML_POLICY_STORE)
            .expect("failed to deserialize policy-store_readable.yaml into LegacyAgamaPolicyStore");
    }
}
