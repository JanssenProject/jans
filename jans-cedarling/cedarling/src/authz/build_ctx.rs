// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::AuthzConfig;
use crate::common::cedar_schema::cedar_json::CedarSchemaJson;
use crate::common::cedar_schema::cedar_json::attribute::Attribute;
use crate::entity_builder::BuiltEntities;
use cedar_policy::Entity;
use cedar_policy::EntityTypeName;
use serde_json::{Value, json, map::Entry};
use smol_str::ToSmolStr;
use std::collections::HashMap;

use super::errors::BuildContextError;

/// Constructs the authorization context by dispatching to schema or no-schema implementation.
///
/// When a schema is available, entity references from tokens are injected into the context
/// based on the action's context shape. Without a schema, only request context and pushed
/// data are merged.
pub(super) fn build_context(
    config: &AuthzConfig,
    request_context: Value,
    build_entities: &BuiltEntities,
    action: &cedar_policy::EntityUid,
    pushed_data: HashMap<String, Value>,
) -> Result<cedar_policy::Context, BuildContextError> {
    if config.policy_store.schema.is_some() {
        build_context_with_schema(config, request_context, build_entities, action, pushed_data)
    } else {
        build_context_no_schema(request_context, pushed_data)
    }
}

/// Constructs the authorization context using schema-based entity references.
///
/// Builds context by looking up the action's expected context shape in the Cedar schema
/// and injecting matching entity references from the built entities.
pub(super) fn build_context_with_schema(
    config: &AuthzConfig,
    request_context: Value,
    build_entities: &BuiltEntities,
    action: &cedar_policy::EntityUid,
    pushed_data: HashMap<String, Value>,
) -> Result<cedar_policy::Context, BuildContextError> {
    let store_schema = config.policy_store.schema.as_ref()
        .ok_or_else(|| BuildContextError::ContextCreation(
            "build_context_with_schema called but policy store has no schema".to_string()
        ))?;
    let json_schema = &store_schema.json;
    let schema = &store_schema.schema;
    let namespace = action.type_name().namespace();
    let action_name = &action.id().escaped();

    let action_schema = json_schema
        .get_action(&namespace, action_name)
        .ok_or(BuildContextError::UnknownAction(action_name.to_string()))?;

    let mut ctx_entity_refs = json!({});

    if let Some(ctx) = action_schema.applies_to.context.as_ref() {
        match ctx {
            Attribute::Record { attrs, .. } => {
                for (key, attr) in attrs {
                    if let Some(entity_ref) =
                        build_entity_refs_from_attr(&namespace, attr, build_entities, json_schema)?
                    {
                        ctx_entity_refs[key] = entity_ref;
                    }
                }
            },
            Attribute::EntityOrCommon { name, .. } => {
                if let Some((_entity_type_name, attr)) = json_schema
                    .get_common_type(name, Some(&namespace))
                    .map_err(|err| BuildContextError::ParseEntityName(name.clone(), err))?
                {
                    match attr {
                        Attribute::Record { attrs, .. } => {
                            for (key, attr) in attrs {
                                if let Some(entity_ref) = build_entity_refs_from_attr(
                                    &namespace,
                                    attr,
                                    build_entities,
                                    json_schema,
                                )? {
                                    ctx_entity_refs[key] = entity_ref;
                                }
                            }
                        },
                        attr => {
                            return Err(BuildContextError::InvalidKind(
                                attr.kind_str().to_string(),
                                "record".to_string(),
                            ));
                        },
                    }
                }
            },
            attr => {
                return Err(BuildContextError::InvalidKind(
                    attr.kind_str().to_string(),
                    "record or common".to_string(),
                ));
            },
        }
    }

    // Inject pushed data under context.data namespace, merging so key conflicts are surfaced.
    if !pushed_data.is_empty() {
        let data_value = Value::Object(serde_json::Map::from_iter(pushed_data));
        let pushed_wrapper = json!({ "data": data_value });
        ctx_entity_refs = merge_json_values(ctx_entity_refs, &pushed_wrapper)?;
    }

    let context = merge_json_values(request_context, &ctx_entity_refs)?;
    let context: cedar_policy::Context =
        cedar_policy::Context::from_json_value(context, Some((schema, action)))
            .map_err(|e| BuildContextError::ContextCreation(e.to_string()))?;

    Ok(context)
}

/// Constructs the authorization context without schema-based entity references.
///
/// Merges request context and pushed data directly. Entity references are not injected
/// because the schema is unavailable.
pub(super) fn build_context_no_schema(
    request_context: Value,
    pushed_data: HashMap<String, Value>,
) -> Result<cedar_policy::Context, BuildContextError> {
    let mut context_json = request_context;
    if !pushed_data.is_empty() {
        let data_value = Value::Object(serde_json::Map::from_iter(pushed_data));
        let pushed_wrapper = json!({ "data": data_value });
        context_json = merge_json_values(context_json, &pushed_wrapper)?;
    }

    cedar_policy::Context::from_json_value(context_json, None)
        .map_err(|e| BuildContextError::ContextCreation(e.to_string()))
}

/// Constructs the authorization context for multi-issuer requests with token collection
///
/// This function implements the design document's token collection context structure:
/// - Individual token entities are accessible via `context.tokens.{issuer}_{token_type}`
/// - All JWT claims are stored as entity tags (Set of String by default)
/// - Provides ergonomic policy syntax for cross-token validation
///
/// ## Context Merging
///
/// Values are merged in the following order:
/// 1. **Request Context** - Values passed directly in the authorization request
/// 2. **Token References** - Built token entity references
/// 3. **Pushed Data** - Data from the `DataStore` (injected under `context.data`)
///
/// **Important:** Merges will error on key conflicts via `merge_json_values`, which returns
/// `BuildContextError::KeyConflict` when any key collision occurs. Callers must resolve
/// conflicts before invoking this function.
pub(super) fn build_multi_issuer_context(
    request_context: Value,
    token_entities: &HashMap<String, Entity>,
    schema: Option<&cedar_policy::Schema>,
    action: &cedar_policy::EntityUid,
    pushed_data: HashMap<String, Value>,
) -> Result<cedar_policy::Context, BuildContextError> {
    // Start with the request context
    let mut context_json = request_context;

    // Create the tokens context as specified in the design document
    let mut tokens_context = serde_json::Map::new();

    // Add individual token entity references to context
    // Format: {issuer}_{token_type} -> {"type": "EntityType", "id": "entity_id"}
    for (field_name, entity) in token_entities {
        // Create entity reference like the existing build_context function
        let entity_ref = json!({
            "type": entity.uid().type_name().to_string(),
            "id": entity.uid().id().as_ref() as &str
        });
        tokens_context.insert(field_name.clone(), entity_ref);
    }

    // Add total token count as specified in design
    tokens_context.insert("total_token_count".to_string(), json!(token_entities.len()));

    // Merge tokens into request context, checking for conflicts
    let tokens_value = Value::Object(tokens_context);
    let tokens_wrapper = json!({ "tokens": tokens_value });
    context_json = merge_json_values(context_json, &tokens_wrapper)?;

    // Inject pushed data under context.data namespace, merging so key conflicts are surfaced.
    if !pushed_data.is_empty() {
        let data_value = Value::Object(serde_json::Map::from_iter(pushed_data));
        let pushed_wrapper = json!({ "data": data_value });
        context_json = merge_json_values(context_json, &pushed_wrapper)?;
    }

    // Create Cedar context
    let context = cedar_policy::Context::from_json_value(context_json, schema.map(|s| (s, action)))
        .map_err(|e| BuildContextError::ContextCreation(e.to_string()))?;

    Ok(context)
}

/// Builds the JSON entity references from a given attribute.
///
/// Returns `Ok(None)` if the attr is not an Entity Reference
fn build_entity_refs_from_attr(
    namespace: &str,
    attr: &Attribute,
    built_entities: &BuiltEntities,
    schema: &CedarSchemaJson,
) -> Result<Option<Value>, BuildContextError> {
    match attr {
        Attribute::Entity { name, required } => {
            let Some((type_name, _type_schema)) =
                schema
                    .get_entity_schema(name, Some(namespace))
                    .map_err(|e| BuildContextError::ParseEntityName(name.clone(), e))?
            else {
                return Ok(None);
            };

            match map_entity_id(&type_name, built_entities) {
                Ok(res) => Ok(res),
                Err(err) => {
                    if *required {
                        Err(err)
                    } else {
                        Ok(None)
                    }
                },
            }
        },
        Attribute::EntityOrCommon { name, required } => {
            let Some((type_name, _type_schema)) =
                schema
                    .get_entity_schema(name, Some(namespace))
                    .map_err(|e| BuildContextError::ParseEntityName(name.clone(), e))?
            else {
                return Ok(None);
            };

            match map_entity_id(&type_name, built_entities) {
                Ok(res) => Ok(res),
                Err(err) => {
                    if *required {
                        Err(err)
                    } else {
                        Ok(None)
                    }
                },
            }
        },
        _ => Ok(None),
    }
}

/// Maps a known entity ID to the entity reference
fn map_entity_id(
    name: &EntityTypeName,
    built_entities: &BuiltEntities,
) -> Result<Option<Value>, BuildContextError> {
    if let Some(type_id) = built_entities.get_single(&name.to_smolstr()) {
        Ok(Some(json!({"type": name.to_string(), "id": type_id})))
    } else {
        Err(BuildContextError::MissingEntityId(name.to_string()))
    }
}

fn merge_json_values(mut base: Value, other: &Value) -> Result<Value, BuildContextError> {
    if let (Some(base_map), Some(additional_map)) = (base.as_object_mut(), other.as_object()) {
        for (key, value) in additional_map {
            if let Entry::Vacant(entry) = base_map.entry(key) {
                entry.insert(value.clone());
            } else {
                return Err(BuildContextError::KeyConflict(key.clone()));
            }
        }
    }
    Ok(base)
}

#[cfg(test)]
mod test {
    use super::*;
    use serde_json::json;
    use std::str::FromStr;

    #[test]
    fn can_merge_json_objects() {
        let obj1 = json!({ "a": 1, "b": 2 });
        let obj2 = json!({ "c": 3, "d": 4 });
        let expected = json!({"a": 1, "b": 2, "c": 3, "d": 4});

        let result = merge_json_values(obj1, &obj2).expect("Should merge JSON objects");

        assert_eq!(result, expected);
    }

    #[test]
    fn errors_on_same_keys() {
        let obj1 = json!({ "a": 1, "b": 2 });
        let obj2 = json!({ "b": 3, "c": 4 });
        let result = merge_json_values(obj1, &obj2);

        assert!(
            matches!(result, Err(BuildContextError::KeyConflict(key)) if key.as_str() == "b"),
            "Expected an error due to conflicting keys"
        );
    }

    #[test]
    fn build_context_no_schema_merges_context_and_pushed_data() {
        let request_ctx = json!({ "req": "value" });
        let pushed = HashMap::from([("key".to_string(), json!("val"))]);

        let result = build_context_no_schema(request_ctx, pushed);
        let context = result.expect("build_context_no_schema should succeed without schema");

        let ctx_json = context.to_json_value().expect("context should serialize to json");
        assert_eq!(
            ctx_json.get("req").and_then(|v| v.as_str()),
            Some("value"),
            "context should contain request key"
        );
        assert!(
            ctx_json.get("data").is_some(),
            "context should contain pushed data under 'data' key"
        );
    }

    #[test]
    fn build_context_no_schema_without_pushed_data() {
        let request_ctx = json!({ "req": "value" });
        let pushed = HashMap::new();

        let result = build_context_no_schema(request_ctx.clone(), pushed);
        let context = result.expect("build_context_no_schema should work without pushed data");

        let ctx_json = context.to_json_value().expect("context should serialize to json");
        assert_eq!(
            ctx_json,
            request_ctx,
            "context should equal request context unchanged"
        );
    }

    #[test]
    fn build_context_no_schema_errors_on_key_conflict_with_pushed_data() {
        let request_ctx = json!({ "data": "my_value" });
        let pushed = HashMap::from([("conflict".to_string(), json!("val"))]);

        let result = build_context_no_schema(request_ctx, pushed);
        let err = result
            .expect_err("should error on key conflict between request context and 'data' wrapper");
        assert!(
            matches!(&err, BuildContextError::KeyConflict(key) if key == "data"),
            "expected KeyConflict on 'data' key, got: {err:?}"
        );
    }

    #[test]
    fn build_multi_issuer_context_without_schema() {
        let request_ctx = json!({ "req": "value" });
        let token_entities = HashMap::new();
        let action = cedar_policy::EntityUid::from_str("Test::Action::\"act\"")
            .expect("valid action uid");

        let result = build_multi_issuer_context(
            request_ctx.clone(),
            &token_entities,
            None,
            &action,
            HashMap::new(),
        );
        let context = result
            .expect("build_multi_issuer_context should succeed without schema");

        let ctx_json = context.to_json_value().expect("context should serialize to json");
        assert_eq!(
            ctx_json.get("req").and_then(|v| v.as_str()),
            Some("value"),
            "context should preserve request fields"
        );
        assert!(
            ctx_json.get("tokens").is_some(),
            "context should have tokens wrapper"
        );
    }

    #[test]
    fn build_multi_issuer_context_without_schema_includes_token_refs() {
        let entity = Entity::from_json_str(
            r#"{"uid":{"type":"Jans::access_token","id":"tok1"},"attrs":{},"parents":[]}"#,
            None,
        )
        .expect("valid entity");
        let mut token_entities = HashMap::new();
        token_entities.insert("jans_access_token".to_string(), entity);

        let request_ctx = json!({});
        let action = cedar_policy::EntityUid::from_str("Jans::Action::\"act\"")
            .expect("valid action uid");

        let result = build_multi_issuer_context(
            request_ctx,
            &token_entities,
            None,
            &action,
            HashMap::new(),
        );
        let context = result
            .expect("build_multi_issuer_context with tokens should succeed");

        let ctx_json = context.to_json_value().expect("context should serialize to json");
        let tokens = ctx_json
            .get("tokens")
            .expect("context should have tokens");
        assert!(
            tokens.get("jans_access_token").is_some(),
            "token entity reference should be in context"
        );
        assert_eq!(
            tokens
                .get("total_token_count")
                .and_then(serde_json::Value::as_u64),
            Some(1),
            "total_token_count should be 1"
        );
    }
}
