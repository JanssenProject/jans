// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::attribute::Attribute;
use super::{ActionGroupName, EntityName};
use serde::Deserialize;
use std::collections::HashSet;

#[derive(Debug, Deserialize, Clone)]
#[cfg_attr(test, derive(PartialEq))]
pub(crate) struct Action {
    #[serde(rename = "memberOf", default)]
    #[allow(dead_code)]
    member_of: Option<HashSet<ActionGroup>>,
    #[serde(rename = "appliesTo")]
    pub applies_to: AppliesTo,
}

#[derive(Debug, Deserialize, Hash, PartialEq, Eq, Clone)]
struct ActionGroup {
    id: EntityName,
    /// Specifies membership for an action group in a different namespace.
    ///
    /// e.g.: `kind: "My::Namespace::Action"`
    #[serde(rename = "type", default)]
    kind: Option<ActionGroupName>,
}

#[derive(Debug, Deserialize, Clone)]
#[cfg_attr(test, derive(PartialEq))]
pub(crate) struct AppliesTo {
    #[serde(default)]
    pub context: Option<Attribute>,
}

#[cfg(test)]
mod test_deserialize_action {
    use super::super::attribute::Attribute;
    use super::{Action, ActionGroup, AppliesTo};
    use serde_json::json;
    use std::collections::{HashMap, HashSet};
    use test_utils::assert_eq;

    #[test]
    fn can_deserialize() {
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
                applies_to: AppliesTo { context: None },
            }
        );
    }

    #[test]
    fn can_deserialize_with_member_of() {
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
                applies_to: AppliesTo { context: None },
            }
        );

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
                applies_to: AppliesTo { context: None },
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
                    context: Some(Attribute::record(HashMap::from([
                        ("field1".into(), Attribute::boolean()),
                        ("field2".into(), Attribute::long()),
                        ("field3".into(), Attribute::String)
                    ]))),
                },
            }
        );
    }
}
