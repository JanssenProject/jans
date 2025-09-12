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
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct EntityData {
    /// Cedar entity mapping info
    #[serde(rename = "cedar_entity_mapping")]
    pub cedar_mapping: CedarEntityMapping,
    /// entity attributes
    #[serde(flatten)]
    pub attributes: HashMap<String, Value>,
}

/// Cedar entity mapping information
#[derive(Serialize, Deserialize, Debug, Clone)]
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

#[allow(dead_code)]
impl TokenInput {
    /// Create a new TokenInput
    pub fn new(mapping: String, payload: String) -> Self {
        Self { mapping, payload }
    }

    /// Validate the token input
    pub fn validate(&self) -> Result<(), TokenInputError> {
        // Check if mapping is empty
        if self.mapping.trim().is_empty() {
            return Err(TokenInputError::EmptyMapping);
        }

        // Check if mapping follows the expected format "Namespace::TokenType"
        if !self.mapping.contains("::") {
            return Err(TokenInputError::InvalidMappingFormat {
                mapping: self.mapping.clone(),
            });
        }

        // Check if payload is empty
        if self.payload.trim().is_empty() {
            return Err(TokenInputError::EmptyPayload);
        }

        // Basic JWT format check - should contain exactly 2 dots
        let dot_count = self.payload.matches('.').count();
        if dot_count != 2 {
            return Err(TokenInputError::InvalidJwtFormat);
        }

        Ok(())
    }
}

/// Multi-issuer authorization request
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct AuthorizeMultiIssuerRequest {
    /// Array of JWT tokens with explicit type mappings
    pub tokens: Vec<TokenInput>,
    /// Optional resource being accessed (JSON format)
    pub resource: Option<Value>,
    /// Optional action being performed (JSON format)
    pub action: Option<Value>,
    /// Optional additional context for policy evaluation (JSON format)
    pub context: Option<Value>,
}

#[allow(dead_code)]
impl AuthorizeMultiIssuerRequest {
    /// Create a new AuthorizeMultiIssuerRequest
    pub fn new(tokens: Vec<TokenInput>) -> Self {
        Self {
            tokens,
            resource: None,
            action: None,
            context: None,
        }
    }

    /// Create a new AuthorizeMultiIssuerRequest with all fields
    pub fn new_with_fields(
        tokens: Vec<TokenInput>,
        resource: Option<Value>,
        action: Option<Value>,
        context: Option<Value>,
    ) -> Self {
        Self {
            tokens,
            resource,
            action,
            context,
        }
    }

    /// Validate the request
    pub fn validate(&self) -> Result<(), MultiIssuerValidationError> {
        // Check if tokens array is empty
        if self.tokens.is_empty() {
            return Err(MultiIssuerValidationError::EmptyTokenArray);
        }

        // Validate each token
        for token in &self.tokens {
            token.validate().map_err(MultiIssuerValidationError::from)?;
        }

        // Validate JSON fields if provided
        if let Some(ref resource) = self.resource {
            if !resource.is_object() && !resource.is_string() {
                return Err(MultiIssuerValidationError::InvalidResourceJson);
            }
        }

        if let Some(ref action) = self.action {
            if !action.is_string() {
                return Err(MultiIssuerValidationError::InvalidActionJson);
            }
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

    #[test]
    fn test_token_input_creation() {
        let token = TokenInput::new(
            "Jans::Access_Token".to_string(),
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c".to_string(),
        );

        assert_eq!(token.mapping, "Jans::Access_Token");
        assert!(token.payload.starts_with("eyJ"));
    }

    #[test]
    fn test_token_input_validation_success() {
        let token = TokenInput::new(
            "Jans::Access_Token".to_string(),
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c".to_string(),
        );

        assert!(token.validate().is_ok());
    }

    #[test]
    fn test_token_input_validation_empty_mapping() {
        let token = TokenInput::new("".to_string(), "valid.jwt.token".to_string());

        let result = token.validate();
        assert!(matches!(result, Err(TokenInputError::EmptyMapping)));
    }

    #[test]
    fn test_token_input_validation_invalid_mapping_format() {
        let token = TokenInput::new("InvalidFormat".to_string(), "valid.jwt.token".to_string());

        let result = token.validate();
        assert!(
            matches!(result, Err(TokenInputError::InvalidMappingFormat { mapping }) if mapping == "InvalidFormat")
        );
    }

    #[test]
    fn test_token_input_validation_empty_payload() {
        let token = TokenInput::new("Jans::Access_Token".to_string(), "".to_string());

        let result = token.validate();
        assert!(matches!(result, Err(TokenInputError::EmptyPayload)));
    }

    #[test]
    fn test_token_input_validation_invalid_jwt_format() {
        let token = TokenInput::new("Jans::Access_Token".to_string(), "invalid-jwt".to_string());

        let result = token.validate();
        assert!(matches!(result, Err(TokenInputError::InvalidJwtFormat)));
    }

    #[test]
    fn test_authorize_multi_issuer_request_creation() {
        let tokens = vec![
            TokenInput::new(
                "Jans::Access_Token".to_string(),
                "valid.jwt.token".to_string(),
            ),
            TokenInput::new("Jans::Id_Token".to_string(), "valid.jwt.token".to_string()),
        ];

        let request = AuthorizeMultiIssuerRequest::new(tokens.clone());

        assert_eq!(request.tokens.len(), 2);
        assert!(request.resource.is_none());
        assert!(request.action.is_none());
        assert!(request.context.is_none());
    }

    #[test]
    fn test_authorize_multi_issuer_request_with_fields() {
        let tokens = vec![TokenInput::new(
            "Jans::Access_Token".to_string(),
            "valid.jwt.token".to_string(),
        )];

        let resource = Some(json!({"type": "Document", "id": "doc123"}));
        let action = Some(json!("Read"));
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
            TokenInput::new(
                "Jans::Access_Token".to_string(),
                "valid.jwt.token".to_string(),
            ),
            TokenInput::new("Jans::Id_Token".to_string(), "valid.jwt.token".to_string()),
        ];

        let request = AuthorizeMultiIssuerRequest::new(tokens);

        assert!(request.validate().is_ok());
    }

    #[test]
    fn test_authorize_multi_issuer_request_validation_empty_tokens() {
        let request = AuthorizeMultiIssuerRequest::new(vec![]);

        let result = request.validate();
        assert!(matches!(
            result,
            Err(MultiIssuerValidationError::EmptyTokenArray)
        ));
    }

    #[test]
    fn test_authorize_multi_issuer_request_validation_invalid_token() {
        let tokens = vec![TokenInput::new(
            "InvalidFormat".to_string(),
            "valid.jwt.token".to_string(),
        )];

        let request = AuthorizeMultiIssuerRequest::new(tokens);

        let result = request.validate();
        assert!(matches!(
            result,
            Err(MultiIssuerValidationError::TokenInput(
                TokenInputError::InvalidMappingFormat { .. }
            ))
        ));
    }

    #[test]
    fn test_authorize_multi_issuer_request_validation_invalid_json_fields() {
        let tokens = vec![TokenInput::new(
            "Jans::Access_Token".to_string(),
            "valid.jwt.token".to_string(),
        )];

        let request = AuthorizeMultiIssuerRequest::new_with_fields(
            tokens,
            Some(json!(123)), // Invalid resource (should be object or string)
            Some(json!(123)), // Invalid action (should be string)
            Some(json!(123)), // Invalid context (should be object)
        );

        let result = request.validate();
        assert!(matches!(
            result,
            Err(MultiIssuerValidationError::InvalidResourceJson)
        ));
    }

    #[test]
    fn test_serialization_deserialization() {
        let tokens = vec![TokenInput::new(
            "Jans::Access_Token".to_string(),
            "valid.jwt.token".to_string(),
        )];

        let request = AuthorizeMultiIssuerRequest::new_with_fields(
            tokens,
            Some(json!({"type": "Document"})),
            Some(json!("Read")),
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
