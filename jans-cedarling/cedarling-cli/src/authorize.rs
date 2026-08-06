// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use anyhow::{Context, Result};
use cedarling::{CedarEntityMapping, Cedarling, EntityData, RequestUnsigned};
use std::collections::HashMap;

use clap::Args;

/// Arguments for the authorize command.
#[derive(Args)]
pub struct AuthorizeArgs {
    /// The entity type of the principal
    #[arg(long, requires = "principal_id")]
    pub principal_type: Option<String>,
    /// The unique ID of the principal
    #[arg(long, requires = "principal_type")]
    pub principal_id: Option<String>,
    /// Optional JSON string containing principal attributes
    #[arg(long)]
    pub principal_attrs: Option<String>,
    /// The action to evaluate
    #[arg(long)]
    pub action: String,
    /// The entity type of the resource
    #[arg(long)]
    pub resource_type: String,
    /// The unique ID of the resource
    #[arg(long)]
    pub resource_id: String,
    /// Optional JSON string containing resource attributes
    #[arg(long)]
    pub resource_attrs: Option<String>,
    /// Optional JSON string containing contextual data
    #[arg(long, default_value = "{}")]
    pub context: String,
}

fn build_entity(
    typ: String,
    id: String,
    attrs: Option<String>,
    name: &str,
) -> Result<EntityData> {
    let attributes = if let Some(a) = attrs {
        serde_json::from_str(&a).with_context(|| format!("failed to parse {name} as JSON"))?
    } else {
        HashMap::new()
    };
    Ok(EntityData {
        cedar_mapping: CedarEntityMapping { entity_type: typ, id },
        attributes,
    })
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
            Some(build_entity(typ, id, args.principal_attrs, "principal-attrs")?)
        },
        (None, None) => {
            if args.principal_attrs.is_some() {
                anyhow::bail!("--principal-attrs cannot be provided without --principal-type and --principal-id");
            }
            None
        },
        _ => anyhow::bail!("Both --principal-type and --principal-id must be provided together"),
    };

    let resource = build_entity(
        args.resource_type,
        args.resource_id,
        args.resource_attrs,
        "resource-attrs",
    )?;

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
