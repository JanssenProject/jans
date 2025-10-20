// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;

use serde::{Deserialize, Deserializer, Serialize, de};
use serde_json::Value;

use super::errors::{MultiIssuerValidationError, TokenInputError};

/// Box to store authorization data
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Request {
    /// Contains the JWTs that will be used for the AuthZ request
    #[serde(default, deserialize_with = "deserialize_tokens")]
    pub tokens: HashMap<String, String>,
    /// cedar_policy action
    pub action: String,
    /// cedar_policy resource data
    pub resource: EntityData,
    /// context to be used in cedar_policy
    pub context: Value,
}

/// Custom parser for an Option<String> which returns `None` if the string is empty.
fn deserialize_tokens<'de, D>(deserializer: D) -> Result<HashMap<String, String>, D::Error>
where
    D: Deserializer<'de>,
{
    let tokens = HashMap::<String, Value>::deserialize(deserializer)?;
    let (tokens, errs): (Vec<_>, Vec<_>) = tokens
        .into_iter()
        .filter_map(|(tkn_name, val)| match val {
            Value::Null => None,
            Value::String(token) => Some(Ok((tkn_name, token))),
            val => Some(Err((tkn_name, value_to_str(&val)))),
        })
        .partition(Result::is_ok);

    let tokens: HashMap<String, String> = if errs.is_empty() {
        tokens.into_iter().flatten().collect()
    } else {
        let err_msgs = errs
            .into_iter()
            .map(|e| e.unwrap_err())
            .map(|(tkn_name, got_type)| {
                format!(
                    "expected `{}` to be 'string' or 'null' but got '{}'",
                    tkn_name, got_type
                )
            })
            .collect::<Vec<_>>();
        return Err(de::Error::custom(format!(
            "failed to deserialize input tokens: {:?}",
            err_msgs
        )));
    };

    Ok(tokens)
}

fn value_to_str(value: &Value) -> &'static str {
    match value {
        Value::Null => "null",
        Value::Bool(_) => "bool",
        Value::Number(_) => "number",
        Value::String(_) => "string",
        Value::Array(_) => "array",
        Value::Object(_) => "object",
    }
}

/// Box to store authorization data, with any additional principals
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
pub struct RequestUnsigned {
    /// Contains the JWTs that will be used for the AuthZ request
    pub principals: Vec<EntityData>,
    /// cedar_policy action
    pub action: String,
    /// cedar_policy resource data
    pub resource: EntityData,
    /// context to be used in cedar_policy
    pub context: Value,
}

/// Cedar policy entity data
/// fields represent EntityUid
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
    /// Token mapping type (e.g., "Jans::Access_Token", "Acme::DolphinToken")
    pub mapping: String,
    /// JWT token string
    pub payload: String,
}

impl TokenInput {
    /// Create a new TokenInput
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
    /// Create a new AuthorizeMultiIssuerRequest
    pub fn new(tokens: Vec<TokenInput>, resource: EntityData, action: String) -> Self {
        Self {
            tokens,
            resource,
            action,
            context: None,
        }
    }

    /// Create a new AuthorizeMultiIssuerRequest with all fields
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

        if let Some(ref context) = self.context {
            if !context.is_object() {
                return Err(MultiIssuerValidationError::InvalidContextJson);
            }
        }

        Ok(())
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
            "iat": 1516239022,
            "iss": issuer
        });
        let token_string = generate_token_using_claims(&claims);
        TokenInput::new(mapping.to_string(), token_string)
    }

    #[test]
    fn test_token_input_creation() {
        let token = create_test_token("Jans::Access_Token", "https://example.com", "1234567890");

        assert_eq!(token.mapping, "Jans::Access_Token");
        assert!(token.payload.contains(".")); // JWT format check
    }

    #[test]
    fn test_token_input_validate_success() {
        let token = create_test_token("Jans::Access_Token", "https://example.com", "1234567890");

        let result = token.validate();
        assert!(result.is_ok());
    }

    #[test]
    fn test_token_input_validate_empty_mapping() {
        let token = TokenInput::new("".to_string(), "valid.jwt.token".to_string());

        let result = token.validate();
        assert!(matches!(result, Err(TokenInputError::EmptyMapping)));
    }

    #[test]
    fn test_token_input_validate_empty_payload() {
        let token = TokenInput::new("Jans::Access_Token".to_string(), "".to_string());

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
}
