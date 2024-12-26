// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::record_attr::RecordAttr;
use serde::Deserialize;
use std::collections::HashSet;

pub type ActionName = String;
pub type EntityName = String;
pub type ActionGroupName = String;

#[derive(Debug, Deserialize, PartialEq)]
pub struct Action {
    #[serde(rename = "memberOf", default)]
    member_of: Option<HashSet<ActionGroup>>,
    #[serde(rename = "appliesTo")]
    applies_to: AppliesTo,
}

#[derive(Debug, Deserialize, Hash, PartialEq, Eq)]
pub struct ActionGroup {
    id: EntityName,
    /// Specifies membership for an action group in a different namespace.
    ///
    /// e.g.: `kind: "My::Namespace::Action"`
    #[serde(rename = "type", default)]
    kind: Option<ActionGroupName>,
}

#[derive(Debug, Deserialize, PartialEq)]
pub struct AppliesTo {
    #[serde(rename = "principalTypes", default)]
    principal_types: HashSet<EntityName>,
    #[serde(rename = "resourceTypes", default)]
    resource_types: HashSet<EntityName>,
    #[serde(default)]
    context: Option<RecordAttr>,
}

#[cfg(test)]
mod test_deserialize_action {
    use super::super::record_attr::RecordAttr;
    use super::{Action, ActionGroup, AppliesTo};
    use serde_json::json;
    use std::collections::{HashMap, HashSet};
    use test_utils::assert_eq;

    #[test]
    fn can_deserialize() {
        // Case: both principal types and resource types is empty
        let action = json!({
            "appliesTo": {
                "principalTypes": [],
                "resourceTypes": [],
            }
        });
        let action = serde_json::from_value::<Action>(action).unwrap();
        assert_eq!(
            action,
            Action {
                member_of: None,
                applies_to: AppliesTo {
                    principal_types: HashSet::new(),
                    resource_types: HashSet::new(),
                    context: None,
                },
            }
        );

        // Case: resource types is empty
        let action = json!({
            "appliesTo": {
                "principalTypes": ["PrincipalEntityType1"],
                "resourceTypes": [],
            }
        });
        let action = serde_json::from_value::<Action>(action).unwrap();
        assert_eq!(
            action,
            Action {
                member_of: None,
                applies_to: AppliesTo {
                    principal_types: HashSet::from(["PrincipalEntityType1".into()]),
                    resource_types: HashSet::new(),
                    context: None,
                },
            }
        );

        // Case: only principal types is empty
        let action = json!({
            "appliesTo": {
                "principalTypes": [],
                "resourceTypes": ["ResourceEntityType1"],
            }
        });
        let action = serde_json::from_value::<Action>(action).unwrap();
        assert_eq!(
            action,
            Action {
                member_of: None,
                applies_to: AppliesTo {
                    principal_types: HashSet::new(),
                    resource_types: HashSet::from(["ResourceEntityType1".into()]),
                    context: None,
                },
            }
        );
    }

    #[test]
    fn can_deserialize_with_member_of() {
        // Case: action group type is not provided
        let action = json!({
            "memberOf": [{"id": "read"}],
            "appliesTo": {
                "principalTypes": ["User"],
                "resourceTypes": ["Photo"],
            }
        });
        let action = serde_json::from_value::<Action>(action).unwrap();
        assert_eq!(
            action,
            Action {
                member_of: Some(HashSet::from([ActionGroup {
                    id: "read".into(),
                    kind: None
                }])),
                applies_to: AppliesTo {
                    principal_types: HashSet::from(["User".into()]),
                    resource_types: HashSet::from(["Photo".into()]),
                    context: None,
                },
            }
        );

        // Case: an action group type is provided
        let action = json!({
            "memberOf": [{
                "id": "read",
                "type": "My::Namespace::Action",
            }],
            "appliesTo": {
                "principalTypes": ["User"],
                "resourceTypes": ["Photo"],
            }
        });
        let action = serde_json::from_value::<Action>(action).unwrap();
        assert_eq!(
            action,
            Action {
                member_of: Some(HashSet::from([ActionGroup {
                    id: "read".into(),
                    kind: Some("My::Namespace::Action".into()),
                }])),
                applies_to: AppliesTo {
                    principal_types: HashSet::from(["User".into()]),
                    resource_types: HashSet::from(["Photo".into()]),
                    context: None,
                },
            }
        );
    }

    #[test]
    fn can_deserialize_with_context() {
        let action = json!({
            "appliesTo": {
                "principalTypes": ["PrincipalEntityType1"],
                "resourceTypes": ["ResourceEntityType1"],
                "context": {
                    "type": "Record",
                    "attributes": {
                        "field1": { "type": "Boolean" },
                        "field2": { "type": "Long" },
                        "field3": { "type": "String", "required": false },
                    }
                },
            },
        });
        let action = serde_json::from_value::<Action>(action).unwrap();
        assert_eq!(
            action,
            Action {
                member_of: None,
                applies_to: AppliesTo {
                    principal_types: HashSet::from(["PrincipalEntityType1".into()]),
                    resource_types: HashSet::from(["ResourceEntityType1".into()]),
                    context: Some(RecordAttr::record(HashMap::from([
                        ("field1".into(), RecordAttr::boolean()),
                        ("field2".into(), RecordAttr::long()),
                        ("field3".into(), RecordAttr::String { required: false })
                    ]))),
                },
            }
        );
    }
}
