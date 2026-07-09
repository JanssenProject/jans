// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;

use serde::{Deserialize, Serialize};
use serde_json::Value;
use uuid7::Uuid;

use super::errors::{BatchValidationError, MultiIssuerValidationError, TokenInputError};

/// Authorization request data with an optional principal.
///
/// When `principal` is `None`, the request is evaluated using Cedar's
/// partial-evaluation mode: policies whose principal scope would be unknown
/// can still produce a concrete decision provided they do not depend on the
/// principal's attributes.
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
pub struct RequestUnsigned {
    /// Optional principal entity for the authorization request.
    pub principal: Option<EntityData>,
    /// `cedar_policy` action
    pub action: String,
    /// `cedar_policy` resource data
    pub resource: EntityData,
    /// context to be used in `cedar_policy`
    pub context: Value,
}

/// Cedar policy entity data
/// fields represent `EntityUid`
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq)]
pub struct EntityData {
    /// Cedar entity mapping info
    #[serde(rename = "cedar_entity_mapping")]
    pub cedar_mapping: CedarEntityMapping,
    /// entity attributes
    #[serde(flatten)]
    pub attributes: HashMap<String, Value>,
}

/// Cedar entity mapping information
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq)]
pub struct CedarEntityMapping {
    /// entity type name
    #[serde(rename = "entity_type")]
    pub entity_type: String,
    /// entity id
    pub id: String,
}

impl EntityData {
    /// Deserializes a JSON string into [`EntityData`]
    pub fn from_json(entity_data: &str) -> Result<Self, serde_json::Error> {
        serde_json::from_str::<Self>(entity_data)
    }
}

/// Token input for multi-issuer authorization
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct TokenInput {
    /// Token mapping type (e.g., "`Jans::Access_Token`", "`Acme::DolphinToken`")
    pub mapping: String,
    /// JWT token string
    pub payload: String,
}

impl TokenInput {
    /// Create a new [`TokenInput`]
    #[must_use]
    pub fn new(mapping: String, payload: String) -> Self {
        Self { mapping, payload }
    }

    /// Validate the token input format (mapping and payload presence)
    pub fn validate(&self) -> Result<(), TokenInputError> {
        // Validate mapping format
        if self.mapping.trim().is_empty() {
            return Err(TokenInputError::EmptyMapping);
        }

        // Validate payload format
        if self.payload.trim().is_empty() {
            return Err(TokenInputError::EmptyPayload);
        }

        Ok(())
    }
}

/// Multi-issuer authorization request
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct AuthorizeMultiIssuerRequest {
    /// Array of JWT tokens with explicit type mappings
    pub tokens: Vec<TokenInput>,
    /// Resource being accessed (required for Cedar policy evaluation)
    pub resource: EntityData,
    /// Action being performed (required for Cedar policy evaluation)
    pub action: String,
    /// Optional additional context for policy evaluation (JSON format)
    pub context: Option<Value>,
}

impl AuthorizeMultiIssuerRequest {
    /// Create a new [`AuthorizeMultiIssuerRequest`]
    #[must_use]
    pub fn new(tokens: Vec<TokenInput>, resource: EntityData, action: String) -> Self {
        Self {
            tokens,
            resource,
            action,
            context: None,
        }
    }

    /// Create a new [`AuthorizeMultiIssuerRequest`] with all fields
    #[must_use]
    pub fn new_with_fields(
        tokens: Vec<TokenInput>,
        resource: EntityData,
        action: String,
        context: Option<Value>,
    ) -> Self {
        Self {
            tokens,
            resource,
            action,
            context,
        }
    }

    /// Basic validation of JSON fields
    pub fn validate(&self) -> Result<(), MultiIssuerValidationError> {
        // Basic validation
        if self.tokens.is_empty() {
            return Err(MultiIssuerValidationError::EmptyTokenArray);
        }

        if let Some(ref context) = self.context
            && !context.is_object()
        {
            return Err(MultiIssuerValidationError::InvalidContextJson);
        }

        Ok(())
    }
}

/// A single item in a batch authorization request.
///
/// Each item carries the resource, action, and context that vary per item;
/// the shared principal (unsigned) or token set (multi-issuer) lives on the
/// enclosing batch request.
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq)]
pub struct BatchItem {
    /// Resource being accessed for this item.
    pub resource: EntityData,
    /// Action being performed on this item.
    pub action: String,
    /// Per-item context. Omitting the field defaults to `{}`; explicit
    /// non-object values are rejected via [`BatchValidationError::InvalidItemContext`].
    #[serde(default = "empty_object")]
    pub context: Value,
}

fn empty_object() -> Value {
    Value::Object(serde_json::Map::new())
}

/// Batch unsigned authorization request.
///
/// One optional principal is evaluated against N `{resource, action, context}`
/// items. All items share the same principal snapshot and pushed-data snapshot.
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct BatchAuthorizeUnsignedRequest {
    /// Principal entity for the batch. When `None`, per-item evaluation runs
    /// via Cedar partial evaluation with the same fail-closed contract as
    /// `authorize_unsigned`.
    pub principal: Option<EntityData>,
    /// Items to authorize. Results are returned in the same order.
    pub items: Vec<BatchItem>,
}

impl BatchAuthorizeUnsignedRequest {
    /// Construct a new batch unsigned request.
    #[must_use]
    pub fn new(principal: Option<EntityData>, items: Vec<BatchItem>) -> Self {
        Self { principal, items }
    }

    /// Validate the batch request structure. See [`BatchValidationError`] for
    /// the possible failure modes.
    pub fn validate(&self) -> Result<(), BatchValidationError> {
        if self.items.is_empty() {
            return Err(BatchValidationError::EmptyItems);
        }
        for (index, item) in self.items.iter().enumerate() {
            if !item.context.is_object() {
                return Err(BatchValidationError::InvalidItemContext { index });
            }
        }
        Ok(())
    }
}

/// Batch multi-issuer authorization request.
///
/// One token set is validated once and evaluated against N
/// `{resource, action, context}` items. All items share the same validated-token
/// snapshot, token/issuer entity snapshot, and pushed-data snapshot.
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct BatchAuthorizeMultiIssuerRequest {
    /// JWT tokens with explicit type mappings; validated once for the whole batch.
    pub tokens: Vec<TokenInput>,
    /// Items to authorize. Results are returned in the same order.
    pub items: Vec<BatchItem>,
}

impl BatchAuthorizeMultiIssuerRequest {
    /// Construct a new batch multi-issuer request.
    #[must_use]
    pub fn new(tokens: Vec<TokenInput>, items: Vec<BatchItem>) -> Self {
        Self { tokens, items }
    }

    /// Validate the batch request structure. See [`BatchValidationError`] for
    /// the possible failure modes.
    pub fn validate(&self) -> Result<(), BatchValidationError> {
        if self.tokens.is_empty() {
            return Err(BatchValidationError::EmptyTokens);
        }
        if self.items.is_empty() {
            return Err(BatchValidationError::EmptyItems);
        }
        for (index, item) in self.items.iter().enumerate() {
            if !item.context.is_object() {
                return Err(BatchValidationError::InvalidItemContext { index });
            }
        }
        Ok(())
    }
}

/// Response wrapper for batch authorization calls.
///
/// Carries a shared `batch_id` (`UUIDv7`) alongside per-item results. `results[i]`
/// corresponds to `items[i]` in the request. The `batch_id` matches the
/// `batch_id` field stamped on the per-item decision-log entries emitted
/// during evaluation, so callers can correlate their client-side logs with
/// the server-side audit trail.
///
/// The `batch_id` is also indexed in the in-memory decision-log store — use
/// [`LogStorage::get_logs_by_request_id`](crate::log::interface::LogStorage::get_logs_by_request_id)
/// with `batch_id.to_string()` to retrieve all decision entries for this batch.
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct BatchAuthorizeResponse<R> {
    /// Shared correlation ID for every decision-log entry emitted by this batch.
    pub batch_id: Uuid,
    /// Per-item results, in input order.
    pub results: Vec<R>,
}

impl<R> BatchAuthorizeResponse<R> {
    /// Construct a new batch response.
    #[must_use]
    pub fn new(batch_id: Uuid, results: Vec<R>) -> Self {
        Self { batch_id, results }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;
    use test_utils::token_claims::generate_token_using_claims;

    // Helper function to create test tokens with proper claims
    fn create_test_token(mapping: &str, issuer: &str, sub: &str) -> TokenInput {
        let claims = json!({
            "sub": sub,
            "iat": 1_516_239_022,
            "iss": issuer
        });
        let token_string = generate_token_using_claims(&claims);
        TokenInput::new(mapping.to_string(), token_string)
    }

    #[test]
    fn test_token_input_creation() {
        let token = create_test_token("Jans::Access_Token", "https://example.com", "1234567890");

        assert_eq!(token.mapping, "Jans::Access_Token");
        assert!(token.payload.contains('.')); // JWT format check
    }

    #[test]
    fn test_token_input_validate_success() {
        let token = create_test_token("Jans::Access_Token", "https://example.com", "1234567890");

        let result = token.validate();
        assert!(result.is_ok());
    }

    #[test]
    fn test_token_input_validate_empty_mapping() {
        let token = TokenInput::new(String::new(), "valid.jwt.token".to_string());

        let result = token.validate();
        assert!(matches!(result, Err(TokenInputError::EmptyMapping)));
    }

    #[test]
    fn test_token_input_validate_empty_payload() {
        let token = TokenInput::new("Jans::Access_Token".to_string(), String::new());

        let result = token.validate();
        assert!(matches!(result, Err(TokenInputError::EmptyPayload)));
    }

    #[test]
    fn test_authorize_multi_issuer_request_creation() {
        let tokens = vec![
            create_test_token("Jans::Access_Token", "https://example.com", "1234567890"),
            create_test_token("Jans::Id_Token", "https://example.com", "1234567890"),
        ];

        let resource = EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Document".to_string(),
                id: "doc123".to_string(),
            },
            attributes: HashMap::new(),
        };

        let request =
            AuthorizeMultiIssuerRequest::new(tokens.clone(), resource.clone(), "Read".to_string());

        assert_eq!(request.tokens.len(), 2);
        assert_eq!(request.resource, resource);
        assert_eq!(request.action, "Read");
        assert!(request.context.is_none());
    }

    #[test]
    fn test_authorize_multi_issuer_request_with_fields() {
        let tokens = vec![create_test_token(
            "Jans::Access_Token",
            "https://example.com",
            "1234567890",
        )];

        let resource = EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Document".to_string(),
                id: "doc123".to_string(),
            },
            attributes: HashMap::new(),
        };
        let action = "Read".to_string();
        let context = Some(json!({"location": "miami"}));

        let request = AuthorizeMultiIssuerRequest::new_with_fields(
            tokens,
            resource.clone(),
            action.clone(),
            context.clone(),
        );

        assert_eq!(request.tokens.len(), 1);
        assert_eq!(request.resource, resource);
        assert_eq!(request.action, action);
        assert_eq!(request.context, context);
    }

    #[test]
    fn test_authorize_multi_issuer_request_validation_success() {
        let tokens = vec![
            create_test_token("Jans::Access_Token", "https://example.com", "1234567890"),
            create_test_token("Jans::Id_Token", "https://example.com", "1234567890"),
        ];

        let resource = EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Document".to_string(),
                id: "doc123".to_string(),
            },
            attributes: HashMap::new(),
        };

        let request = AuthorizeMultiIssuerRequest::new(tokens, resource, "Read".to_string());

        assert!(request.validate().is_ok());
    }

    #[test]
    fn test_authorize_multi_issuer_request_validation_empty_tokens() {
        let resource = EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Document".to_string(),
                id: "doc123".to_string(),
            },
            attributes: HashMap::new(),
        };

        let request = AuthorizeMultiIssuerRequest::new(vec![], resource, "Read".to_string());

        let result = request.validate();
        assert!(matches!(
            result,
            Err(MultiIssuerValidationError::EmptyTokenArray)
        ));
    }

    #[test]
    fn test_authorize_multi_issuer_request_validation_invalid_token() {
        let tokens = vec![TokenInput::new(
            "valid-mapping".to_string(), // Valid mapping since we removed token validation
            "some-payload".to_string(),
        )];

        let resource = EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Document".to_string(),
                id: "doc123".to_string(),
            },
            attributes: HashMap::new(),
        };

        let request = AuthorizeMultiIssuerRequest::new(tokens, resource, "Read".to_string());

        let result = request.validate();
        // The new validation logic only checks JSON fields
        // Valid mapping should pass validation
        assert!(result.is_ok());
    }

    #[test]
    fn test_authorize_multi_issuer_request_validation_invalid_json_fields() {
        let tokens = vec![create_test_token(
            "Jans::Access_Token",
            "https://example.com",
            "1234567890",
        )];

        let resource = EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Document".to_string(),
                id: "doc123".to_string(),
            },
            attributes: HashMap::new(),
        };

        let request = AuthorizeMultiIssuerRequest::new_with_fields(
            tokens,
            resource,           // Valid resource
            "Read".to_string(), // Valid action
            Some(json!(123)),   // Invalid context (should be object)
        );

        let result = request.validate();
        assert!(matches!(
            result,
            Err(MultiIssuerValidationError::InvalidContextJson)
        ));
    }

    #[test]
    fn test_serialization_deserialization() {
        let tokens = vec![create_test_token(
            "Jans::Access_Token",
            "https://example.com",
            "1234567890",
        )];

        let resource = EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Document".to_string(),
                id: "doc123".to_string(),
            },
            attributes: HashMap::new(),
        };
        let request = AuthorizeMultiIssuerRequest::new_with_fields(
            tokens,
            resource,
            "Read".to_string(),
            Some(json!({"location": "miami"})),
        );

        // Test serialization
        let json = serde_json::to_string(&request).expect("Should serialize");

        // Test deserialization
        let deserialized: AuthorizeMultiIssuerRequest =
            serde_json::from_str(&json).expect("Should deserialize");

        assert_eq!(request, deserialized);
    }

    fn make_resource(id: &str) -> EntityData {
        EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Document".to_string(),
                id: id.to_string(),
            },
            attributes: HashMap::new(),
        }
    }

    fn make_item(id: &str) -> BatchItem {
        BatchItem {
            resource: make_resource(id),
            action: "Read".to_string(),
            context: json!({}),
        }
    }

    #[test]
    fn batch_unsigned_validates_valid_request() {
        let req = BatchAuthorizeUnsignedRequest::new(None, vec![make_item("a"), make_item("b")]);
        assert!(req.validate().is_ok());
    }

    #[test]
    fn batch_unsigned_rejects_empty_items() {
        let req = BatchAuthorizeUnsignedRequest::new(None, vec![]);
        assert_eq!(req.validate(), Err(BatchValidationError::EmptyItems));
    }

    #[test]
    fn batch_unsigned_rejects_non_object_context() {
        let bad_item = BatchItem {
            resource: make_resource("bad"),
            action: "Read".to_string(),
            context: json!(42),
        };
        let req = BatchAuthorizeUnsignedRequest::new(None, vec![make_item("a"), bad_item]);
        assert_eq!(
            req.validate(),
            Err(BatchValidationError::InvalidItemContext { index: 1 }),
        );
    }

    #[test]
    fn batch_multi_issuer_validates_valid_request() {
        let tokens =
            vec![create_test_token("Jans::Access_Token", "https://example.com", "sub")];
        let req = BatchAuthorizeMultiIssuerRequest::new(tokens, vec![make_item("a")]);
        assert!(req.validate().is_ok());
    }

    #[test]
    fn batch_multi_issuer_rejects_empty_tokens() {
        let req = BatchAuthorizeMultiIssuerRequest::new(vec![], vec![make_item("a")]);
        assert_eq!(req.validate(), Err(BatchValidationError::EmptyTokens));
    }

    #[test]
    fn batch_multi_issuer_rejects_empty_items() {
        let tokens =
            vec![create_test_token("Jans::Access_Token", "https://example.com", "sub")];
        let req = BatchAuthorizeMultiIssuerRequest::new(tokens, vec![]);
        assert_eq!(req.validate(), Err(BatchValidationError::EmptyItems));
    }

    #[test]
    fn batch_multi_issuer_rejects_non_object_context() {
        let tokens =
            vec![create_test_token("Jans::Access_Token", "https://example.com", "sub")];
        let bad_item = BatchItem {
            resource: make_resource("bad"),
            action: "Read".to_string(),
            context: json!("string-not-object"),
        };
        let req = BatchAuthorizeMultiIssuerRequest::new(tokens, vec![bad_item]);
        assert_eq!(
            req.validate(),
            Err(BatchValidationError::InvalidItemContext { index: 0 }),
        );
    }

    #[test]
    fn batch_unsigned_round_trips_json() {
        let req = BatchAuthorizeUnsignedRequest::new(
            Some(make_resource("me")),
            vec![
                BatchItem {
                    resource: make_resource("a"),
                    action: "Read".to_string(),
                    context: json!({"ip": "10.0.0.1"}),
                },
                BatchItem {
                    resource: make_resource("b"),
                    action: "Write".to_string(),
                    context: json!({}),
                },
            ],
        );
        let s = serde_json::to_string(&req).expect("serialize");
        let round: BatchAuthorizeUnsignedRequest =
            serde_json::from_str(&s).expect("deserialize");
        assert_eq!(round.items.len(), 2);
        assert_eq!(round.items[0].action, "Read");
        assert_eq!(round.items[1].action, "Write");
        assert!(round.principal.is_some());
    }

    #[test]
    fn batch_multi_issuer_round_trips_json() {
        let tokens = vec![
            create_test_token("Jans::Access_Token", "https://example.com", "sub-a"),
            create_test_token("Jans::Id_Token", "https://example.com", "sub-b"),
        ];
        let req = BatchAuthorizeMultiIssuerRequest::new(
            tokens,
            vec![make_item("a"), make_item("b"), make_item("c")],
        );
        let s = serde_json::to_string(&req).expect("serialize");
        let round: BatchAuthorizeMultiIssuerRequest =
            serde_json::from_str(&s).expect("deserialize");
        assert_eq!(round.tokens.len(), 2);
        assert_eq!(round.items.len(), 3);
    }

    #[test]
    fn batch_response_round_trips_json() {
        use crate::log::gen_uuid7;
        let response: BatchAuthorizeResponse<String> = BatchAuthorizeResponse::new(
            gen_uuid7(),
            vec!["allow".to_string(), "deny".to_string()],
        );
        let s = serde_json::to_string(&response).expect("serialize");
        let round: BatchAuthorizeResponse<String> =
            serde_json::from_str(&s).expect("deserialize");
        assert_eq!(round.batch_id, response.batch_id);
        assert_eq!(round.results, response.results);
    }
}
