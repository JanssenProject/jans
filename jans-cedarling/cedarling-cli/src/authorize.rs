// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use anyhow::{Context, Result};
use cedarling::{CedarEntityMapping, Cedarling, EntityData, RequestUnsigned};
use std::collections::HashMap;

/// Arguments for the authorize command.
pub struct AuthorizeArgs {
    pub principal_type: Option<String>,
    pub principal_id: Option<String>,
    pub principal_attrs: Option<String>,
    pub action: String,
    pub resource_type: String,
    pub resource_id: String,
    pub resource_attrs: Option<String>,
    pub context: String,
}

/// Runs the authorize command against the provided configuration and arguments.
///
/// # Errors
///
/// Returns an error if Cedarling initialization fails, if JSON parsing fails, or if request construction or authorization fails.
pub async fn run(config: cedarling::BootstrapConfig, args: AuthorizeArgs) -> Result<i32> {
    let cedarling = Cedarling::new(&config)
        .await
        .context("failed to initialize Cedarling")?;

    let principal = match (args.principal_type, args.principal_id) {
        (Some(typ), Some(id)) => {
            let attrs: HashMap<String, serde_json::Value> = if let Some(a) = args.principal_attrs {
                serde_json::from_str(&a).context("failed to parse principal-attrs as JSON")?
            } else {
                HashMap::new()
            };
            Some(EntityData {
                cedar_mapping: CedarEntityMapping {
                    entity_type: typ,
                    id,
                },
                attributes: attrs,
            })
        },
        _ => None,
    };

    let resource_attrs_map: HashMap<String, serde_json::Value> =
        if let Some(a) = args.resource_attrs {
            serde_json::from_str(&a).context("failed to parse resource-attrs as JSON")?
        } else {
            HashMap::new()
        };

    let resource = EntityData {
        cedar_mapping: CedarEntityMapping {
            entity_type: args.resource_type,
            id: args.resource_id,
        },
        attributes: resource_attrs_map,
    };

    let context_val: serde_json::Value =
        serde_json::from_str(&args.context).context("failed to parse context as JSON")?;

    let req = RequestUnsigned {
        principal,
        action: args.action,
        resource,
        context: context_val,
    };

    let result = cedarling
        .authorize_unsigned(req)
        .await
        .context("authorization failed")?;

    let output = serde_json::to_string_pretty(&result)?;
    println!("{output}");

    if result.decision { Ok(0) } else { Ok(1) }
}
