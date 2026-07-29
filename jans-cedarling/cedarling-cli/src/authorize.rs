use anyhow::{bail, Context, Result};
use cedarling::{Cedarling, RequestUnsigned, EntityData, CedarEntityMapping};
use std::collections::HashMap;

pub async fn run(
    config: cedarling::BootstrapConfig,
    principal_type: Option<String>,
    principal_id: Option<String>,
    principal_attrs: Option<String>,
    action: String,
    resource_type: String,
    resource_id: String,
    resource_attrs: Option<String>,
    context: String,
) -> Result<i32> {
    let cedarling = Cedarling::new(&config)
        .await
        .context("failed to initialize Cedarling")?;

    let principal = match (principal_type, principal_id) {
        (Some(typ), Some(id)) => {
            let attrs: HashMap<String, serde_json::Value> = if let Some(a) = principal_attrs {
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
        }
        (None, None) => None,
        _ => bail!("both --principal-type and --principal-id must be provided together or not at all"),
    };

    let r_attrs: HashMap<String, serde_json::Value> = if let Some(a) = resource_attrs {
        serde_json::from_str(&a).context("failed to parse resource-attrs as JSON")?
    } else {
        HashMap::new()
    };

    let resource = EntityData {
        cedar_mapping: CedarEntityMapping {
            entity_type: resource_type,
            id: resource_id,
        },
        attributes: r_attrs,
    };

    let context_val: serde_json::Value = serde_json::from_str(&context)
        .context("failed to parse context as JSON")?;

    let req = RequestUnsigned {
        principal,
        action,
        resource,
        context: context_val,
    };

    let result = cedarling
        .authorize_unsigned(req)
        .await
        .context("authorization failed")?;

    let output = serde_json::to_string_pretty(&result)?;
    println!("{}", output);

    if result.decision {
        Ok(0)
    } else {
        Ok(1)
    }
}
