// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::AuthzConfig;
use crate::common::cedar_schema::CedarSchema;
use crate::common::policy_store::ContextKey;
use crate::common::policy_store::token_types::TOTAL_TOKEN_COUNT_KEY;
use crate::entity_builder::BuiltEntities;
use cedar_policy::Entity;
use cedar_policy_core::validator::types::{EntityKind, Type};
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
    pushed_data: &HashMap<String, Value>,
) -> Result<cedar_policy::Context, BuildContextError> {
    match &config.policy_store.schema {
        Some(schema) => {
            build_context_with_schema(schema, request_context, build_entities, action, pushed_data)
        },
        None => build_context_no_schema(request_context, pushed_data),
    }
}

/// Constructs the authorization context using schema-based entity references.
///
/// Builds context by looking up the action's expected context shape in the Cedar schema
/// and injecting matching entity references from the built entities.
pub(super) fn build_context_with_schema(
    schema: &CedarSchema,
    request_context: Value,
    build_entities: &BuiltEntities,
    action: &cedar_policy::EntityUid,
    pushed_data: &HashMap<String, Value>,
) -> Result<cedar_policy::Context, BuildContextError> {
    let mut ctx_entity_refs = context_entity_refs(schema, action, build_entities)?;

    // Inject pushed data under context.data namespace, merging so key conflicts are surfaced.
    if !pushed_data.is_empty() {
        let data_value = Value::Object(
            pushed_data
                .iter()
                .map(|(k, v)| (k.clone(), v.clone()))
                .collect(),
        );
        let pushed_wrapper = json!({ "data": data_value });
        ctx_entity_refs = merge_json_values(ctx_entity_refs, &pushed_wrapper)?;
    }

    let context = merge_json_values(request_context, &ctx_entity_refs)?;
    let context: cedar_policy::Context =
        cedar_policy::Context::from_json_value(context, Some((&schema.schema, action)))
            .map_err(|e| BuildContextError::ContextCreation(e.to_string()))?;

    Ok(context)
}

/// Returns a JSON object with a reference to a built entity for every attribute of the
/// action's context that is typed as an entity.
///
/// The context type comes from Cedar's validator schema, where common types are already
/// inlined and entity type names fully qualified, so no name resolution is needed here.
fn context_entity_refs(
    schema: &CedarSchema,
    action: &cedar_policy::EntityUid,
    build_entities: &BuiltEntities,
) -> Result<Value, BuildContextError> {
    let context_type = schema
        .validator_schema()
        .context_type(action.as_ref())
        .ok_or_else(|| BuildContextError::UnknownAction(action.id().escaped().to_string()))?;

    let mut ctx_entity_refs = serde_json::Map::new();
    // Cedar always gives an action's context a record type.
    if let Type::Record { attrs, .. } = context_type {
        for (key, attr) in attrs.iter() {
            let Type::Entity(EntityKind::Entity(lub)) = attr.attr_type.as_ref() else {
                continue;
            };
            // Strict validation gives each attribute exactly one entity type.
            let Some(entity_type) = lub.get_single_entity() else {
                continue;
            };
            match map_entity_id(&entity_type.to_smolstr(), build_entities) {
                Ok(entity_ref) => {
                    ctx_entity_refs.insert(key.to_string(), entity_ref);
                },
                Err(err) if attr.is_required => return Err(err),
                Err(_) => {},
            }
        }
    }

    Ok(Value::Object(ctx_entity_refs))
}

/// Constructs the authorization context without schema-based entity references.
///
/// Merges request context and pushed data directly. Entity references are not injected
/// because the schema is unavailable.
pub(super) fn build_context_no_schema(
    request_context: Value,
    pushed_data: &HashMap<String, Value>,
) -> Result<cedar_policy::Context, BuildContextError> {
    let mut context_json = request_context;
    if !pushed_data.is_empty() {
        let data_value = Value::Object(
            pushed_data
                .iter()
                .map(|(k, v)| (k.clone(), v.clone()))
                .collect(),
        );
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
    token_entities: &HashMap<ContextKey, Entity>,
    schema: Option<&cedar_policy::Schema>,
    action: &cedar_policy::EntityUid,
    pushed_data: &HashMap<String, Value>,
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
        tokens_context.insert(field_name.to_string(), entity_ref);
    }

    // Add total token count as specified in design
    tokens_context.insert(
        TOTAL_TOKEN_COUNT_KEY.to_string(),
        json!(token_entities.len()),
    );

    // Merge tokens into request context, checking for conflicts
    let tokens_value = Value::Object(tokens_context);
    let tokens_wrapper = json!({ "tokens": tokens_value });
    context_json = merge_json_values(context_json, &tokens_wrapper)?;

    // Inject pushed data under context.data namespace, merging so key conflicts are surfaced.
    if !pushed_data.is_empty() {
        let data_value = Value::Object(
            pushed_data
                .iter()
                .map(|(k, v)| (k.clone(), v.clone()))
                .collect(),
        );
        let pushed_wrapper = json!({ "data": data_value });
        context_json = merge_json_values(context_json, &pushed_wrapper)?;
    }

    // Create Cedar context
    let context = cedar_policy::Context::from_json_value(context_json, schema.map(|s| (s, action)))
        .map_err(|e| BuildContextError::ContextCreation(e.to_string()))?;

    Ok(context)
}

/// Maps a known entity ID to the entity reference
fn map_entity_id(
    type_name: &str,
    built_entities: &BuiltEntities,
) -> Result<Value, BuildContextError> {
    built_entities
        .get_single(type_name)
        .map(|type_id| json!({"type": type_name, "id": type_id}))
        .ok_or_else(|| BuildContextError::MissingEntityId(type_name.to_string()))
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

    const SCHEMA: &str = r#"
        entity Global;
        namespace Jans {
            entity User;
            entity Workload;
            entity Issue;
            type Ctx = { user: User };
            action "read" appliesTo {
                principal: [User],
                resource: [Issue],
                context: { user: User, workload?: Workload, global?: Global, note?: String }
            };
            action "write" appliesTo { principal: [User], resource: [Issue], context: Ctx };
            action "list" appliesTo { principal: [User], resource: [Issue] };
        }
    "#;

    fn cedar_schema() -> CedarSchema {
        cedar_policy::Schema::from_str(SCHEMA)
            .expect("test schema is valid")
            .into()
    }

    fn action(name: &str) -> cedar_policy::EntityUid {
        cedar_policy::EntityUid::from_type_name_and_id(
            "Jans::Action".parse().expect("valid action type"),
            cedar_policy::EntityId::new(name),
        )
    }

    fn built(uids: &[&str]) -> BuiltEntities {
        uids.iter()
            .map(|uid| cedar_policy::EntityUid::from_str(uid).expect("valid test entity uid"))
            .collect()
    }

    #[test]
    fn context_entity_refs_fills_entity_attrs_of_record_context() {
        let refs = context_entity_refs(
            &cedar_schema(),
            &action("read"),
            &built(&[
                r#"Jans::User::"alice""#,
                r#"Jans::Workload::"w1""#,
                r#"Global::"g""#,
            ]),
        )
        .expect("entity refs should build for a record context");

        assert_eq!(
            refs,
            json!({
                "user": {"type": "Jans::User", "id": "alice"},
                "workload": {"type": "Jans::Workload", "id": "w1"},
                "global": {"type": "Global", "id": "g"},
            }),
            "every entity-typed context attribute gets a reference, including one resolved \
             from the empty namespace; non-entity attributes are left to the request"
        );
    }

    #[test]
    fn context_entity_refs_resolves_common_type_context() {
        let refs = context_entity_refs(
            &cedar_schema(),
            &action("write"),
            &built(&[r#"Jans::User::"alice""#]),
        )
        .expect("entity refs should build for a common-type context");

        assert_eq!(
            refs,
            json!({"user": {"type": "Jans::User", "id": "alice"}}),
            "a context declared as a common type is resolved to its record"
        );
    }

    #[test]
    fn context_entity_refs_skips_missing_optional_entity() {
        let refs = context_entity_refs(
            &cedar_schema(),
            &action("read"),
            &built(&[r#"Jans::User::"alice""#]),
        )
        .expect("a missing optional entity should not fail");

        assert_eq!(
            refs,
            json!({"user": {"type": "Jans::User", "id": "alice"}}),
            "optional entity attributes without a built entity are omitted"
        );
    }

    #[test]
    fn context_entity_refs_errors_on_missing_required_entity() {
        let err = context_entity_refs(
            &cedar_schema(),
            &action("read"),
            &built(&[r#"Jans::Workload::"w1""#]),
        )
        .expect_err("a missing required entity should fail");

        assert!(
            matches!(&err, BuildContextError::MissingEntityId(name) if name == "Jans::User"),
            "expected MissingEntityId for Jans::User, got: {err:?}"
        );
    }

    #[test]
    fn context_entity_refs_errors_on_unknown_action() {
        let err = context_entity_refs(&cedar_schema(), &action("delete"), &built(&[]))
            .expect_err("an action missing from the schema should fail");

        assert!(
            matches!(&err, BuildContextError::UnknownAction(name) if name == "delete"),
            "expected UnknownAction for delete, got: {err:?}"
        );
    }

    #[test]
    fn context_entity_refs_is_empty_for_action_without_context() {
        let refs = context_entity_refs(&cedar_schema(), &action("list"), &built(&[]))
            .expect("an action without context should build");

        assert_eq!(refs, json!({}), "no context means no entity references");
    }

    #[test]
    fn build_context_with_schema_merges_request_context_and_entity_refs() {
        let context = build_context_with_schema(
            &cedar_schema(),
            json!({"note": "hello"}),
            &built(&[r#"Jans::User::"alice""#]),
            &action("read"),
            &HashMap::new(),
        )
        .expect("context should build against the schema");

        assert!(
            context.get("user").is_some(),
            "the entity reference should be injected into the context"
        );
        assert!(
            context.get("note").is_some(),
            "the request context should be kept"
        );
    }

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

        let result = build_context_no_schema(request_ctx, &pushed);
        let context = result.expect("build_context_no_schema should succeed without schema");

        let ctx_json = context
            .to_json_value()
            .expect("context should serialize to json");
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

        let result = build_context_no_schema(request_ctx.clone(), &pushed);
        let context = result.expect("build_context_no_schema should work without pushed data");

        let ctx_json = context
            .to_json_value()
            .expect("context should serialize to json");
        test_utils::assert_eq!(
            ctx_json,
            request_ctx,
            "context should equal request context unchanged"
        );
    }

    #[test]
    fn build_context_no_schema_errors_on_key_conflict_with_pushed_data() {
        let request_ctx = json!({ "data": "my_value" });
        let pushed = HashMap::from([("conflict".to_string(), json!("val"))]);

        let result = build_context_no_schema(request_ctx, &pushed);
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
        let action =
            cedar_policy::EntityUid::from_str("Test::Action::\"act\"").expect("valid action uid");

        let result = build_multi_issuer_context(
            request_ctx.clone(),
            &token_entities,
            None,
            &action,
            &HashMap::new(),
        );
        let context = result.expect("build_multi_issuer_context should succeed without schema");

        let ctx_json = context
            .to_json_value()
            .expect("context should serialize to json");
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
        token_entities.insert(ContextKey::for_test("jans_access_token"), entity);

        let request_ctx = json!({});
        let action =
            cedar_policy::EntityUid::from_str("Jans::Action::\"act\"").expect("valid action uid");

        let result = build_multi_issuer_context(
            request_ctx,
            &token_entities,
            None,
            &action,
            &HashMap::new(),
        );
        let context = result.expect("build_multi_issuer_context with tokens should succeed");

        let ctx_json = context
            .to_json_value()
            .expect("context should serialize to json");
        let tokens = ctx_json.get("tokens").expect("context should have tokens");
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
