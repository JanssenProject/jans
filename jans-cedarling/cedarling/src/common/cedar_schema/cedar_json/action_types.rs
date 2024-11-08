/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use std::collections::HashSet;

/// The Action element describes one action supported by your application.
/// An action consists of a name string,
/// an optional `memberOf`` element, and a required `appliesTo` element.
///
/// The memberOf element specifies what action groups the declared action is a member of in the action hierarchy.
/// The appliesTo element defines the principal types, resource types, and other context information that can be specified in a request for the action.
///
/// Action ::= STR ':' '{' [ '"memberOf"' ':' '[' [ STR { ',' STR } ] ']' ] ',' '"appliesTo"' ':' '{' PrincipalTypes ',' ResourceTypes [',' Context] '}' '}'
//
// memberOf not implemented, because currently is not used
#[derive(Debug, Clone, serde::Deserialize, serde::Serialize, PartialEq)]
pub struct CedarActionElement {
    #[serde(rename = "appliesTo")]
    pub applies_to: CedarActionAppliesTo,
}

/// The appliesTo element defines the principal types, resource types, and other context information that can be specified in a request for the action.
#[derive(Debug, Clone, serde::Deserialize, serde::Serialize, PartialEq)]
pub struct CedarActionAppliesTo {
    #[serde(rename = "resourceTypes")]
    pub resource_types: HashSet<String>,
    #[serde(rename = "principalTypes")]
    pub principal_types: HashSet<String>,
}
