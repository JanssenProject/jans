// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::collections::HashMap;
use std::collections::HashSet;

use serde::{Deserialize, Deserializer, Serialize, de};
use serde_json::Value;

use super::errors::{MultiIssuerValidationError, TokenInputError};
use crate::jwt::decode_jwt;
use crate::log::{LogEntry, LogLevel, LogType, Logger, interface::LogWriter};

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

impl TokenInput {
    /// Create a new TokenInput
    pub fn new(mapping: String, payload: String) -> Self {
        Self { mapping, payload }
    }

    /// Parse and validate the token, returning (token_type, parsed_payload)
    pub fn parse_and_validate(&self) -> Result<(String, Value), TokenInputError> {
        // Validate mapping format
        if self.mapping.trim().is_empty() {
            return Err(TokenInputError::EmptyMapping);
        }

        // Validate payload format
        if self.payload.trim().is_empty() {
            return Err(TokenInputError::EmptyPayload);
        }

        // Parse JWT structure using existing JWT decode function
        let decoded_jwt =
            decode_jwt(&self.payload).map_err(|_| TokenInputError::InvalidJwtFormat)?;

        // Return the claims as a JSON Value
        let parsed_payload = decoded_jwt.claims.inner;

        Ok((self.mapping.clone(), parsed_payload))
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

    /// Comprehensive validation including JWT parsing and non-deterministic token detection
    pub fn validate(&self, logger: &Option<Logger>) -> Result<(), MultiIssuerValidationError> {
        // Basic validation
        if self.tokens.is_empty() {
            return Err(MultiIssuerValidationError::EmptyTokenArray);
        }

        // Validate each token and collect results
        let mut validated_tokens = Vec::new();
        let mut failed_tokens = Vec::new();

        for (index, token) in self.tokens.iter().enumerate() {
            match token.parse_and_validate() {
                Ok((token_type, parsed_payload)) => {
                    // Extract issuer from parsed payload
                    let issuer = self.extract_issuer(&parsed_payload)?;

                    validated_tokens.push(ValidatedToken {
                        index,
                        token_type,
                        issuer,
                        parsed_payload,
                    });
                },
                Err(err) => {
                    failed_tokens.push(FailedToken {
                        index,
                        error: err.to_string(),
                    });
                },
            }
        }

        // If no tokens were successfully validated, return a detailed error
        if validated_tokens.is_empty() {
            // Log detailed error information for each failed token
            for failed in &failed_tokens {
                logger.log_any(
                    LogEntry::new_with_data(LogType::System, None)
                        .set_level(LogLevel::ERROR)
                        .set_message(format!(
                            "Token validation failed at index {} (type: '{}'): {}",
                            failed.index, self.tokens[failed.index].mapping, failed.error
                        )),
                );
            }

            // Collect failed token types for better error message
            let failed_types: Vec<String> = failed_tokens
                .iter()
                .map(|failed| self.tokens[failed.index].mapping.clone())
                .collect();

            return Err(MultiIssuerValidationError::TokenValidationFailed {
                failed_types,
                total_count: self.tokens.len(),
            });
        }

        // Check for non-deterministic tokens (duplicate issuer+type combinations)
        self.check_non_deterministic(&validated_tokens)?;

        // If we have any failed tokens, log them but continue processing
        if !failed_tokens.is_empty() {
            for failed in &failed_tokens {
                logger.log_any(
                    LogEntry::new_with_data(LogType::System, None)
                        .set_level(LogLevel::WARN)
                        .set_message(format!(
                            "Token validation failed at index {}: {}",
                            failed.index, failed.error
                        )),
                );
            }
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

    /// Check for non-deterministic tokens (duplicate issuer+type combinations)
    fn check_non_deterministic(
        &self,
        tokens: &[ValidatedToken],
    ) -> Result<(), MultiIssuerValidationError> {
        let mut seen_combinations = HashSet::new();

        for token in tokens {
            let combination = format!("{}:{}", token.issuer, token.token_type);
            if !seen_combinations.insert(combination.clone()) {
                return Err(MultiIssuerValidationError::NonDeterministicToken {
                    issuer: token.issuer.clone(),
                    token_type: token.token_type.clone(),
                });
            }
        }

        Ok(())
    }

    /// Extract issuer from parsed JWT payload
    fn extract_issuer(&self, payload: &Value) -> Result<String, MultiIssuerValidationError> {
        payload
            .get("iss")
            .and_then(|iss| iss.as_str())
            .ok_or(MultiIssuerValidationError::MissingIssuer)
            .map(|s| s.to_string())
    }
}

/// Validated token with parsed information
#[derive(Debug, Clone)]
pub struct ValidatedToken {
    pub index: usize,
    pub token_type: String,
    pub issuer: String,
    pub parsed_payload: Value,
}

/// Failed token with error information
#[derive(Debug, Clone)]
pub struct FailedToken {
    pub index: usize,
    pub error: String,
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

    // Helper function to create test token without issuer (for missing issuer tests)
    fn create_test_token_no_issuer(mapping: &str, sub: &str) -> TokenInput {
        let claims = json!({
            "sub": sub,
            "iat": 1516239022
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
    fn test_token_input_parse_and_validate_success() {
        let token = create_test_token("Jans::Access_Token", "https://example.com", "1234567890");

        let result = token.parse_and_validate();
        assert!(result.is_ok());

        let (token_type, payload) = result.unwrap();
        assert_eq!(token_type, "Jans::Access_Token");
        assert!(payload.get("sub").is_some());
        assert!(payload.get("iss").is_some());
    }

    #[test]
    fn test_token_input_parse_and_validate_empty_mapping() {
        let token = TokenInput::new("".to_string(), "valid.jwt.token".to_string());

        let result = token.parse_and_validate();
        assert!(matches!(result, Err(TokenInputError::EmptyMapping)));
    }

    #[test]
    fn test_token_input_parse_and_validate_with_namespaced_mapping() {
        // Test that namespaced mappings work correctly
        let token = create_test_token("Jans::Access_Token", "https://example.com", "1234567890");

        let result = token.parse_and_validate();
        assert!(result.is_ok());
        let (token_type, _) = result.unwrap();
        assert_eq!(token_type, "Jans::Access_Token");
    }

    #[test]
    fn test_token_input_parse_and_validate_empty_payload() {
        let token = TokenInput::new("Jans::Access_Token".to_string(), "".to_string());

        let result = token.parse_and_validate();
        assert!(matches!(result, Err(TokenInputError::EmptyPayload)));
    }

    #[test]
    fn test_token_input_parse_and_validate_invalid_jwt_format() {
        let token = TokenInput::new("Jans::Access_Token".to_string(), "invalid-jwt".to_string());

        let result = token.parse_and_validate();
        assert!(matches!(result, Err(TokenInputError::InvalidJwtFormat)));
    }

    #[test]
    fn test_authorize_multi_issuer_request_creation() {
        let tokens = vec![
            create_test_token("Jans::Access_Token", "https://example.com", "1234567890"),
            create_test_token("Jans::Id_Token", "https://example.com", "1234567890"),
        ];

        let request = AuthorizeMultiIssuerRequest::new(tokens.clone());

        assert_eq!(request.tokens.len(), 2);
        assert!(request.resource.is_none());
        assert!(request.action.is_none());
        assert!(request.context.is_none());
    }

    #[test]
    fn test_authorize_multi_issuer_request_with_fields() {
        let tokens = vec![create_test_token(
            "Jans::Access_Token",
            "https://example.com",
            "1234567890",
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
            create_test_token("Jans::Access_Token", "https://example.com", "1234567890"),
            create_test_token("Jans::Id_Token", "https://example.com", "1234567890"),
        ];

        let request = AuthorizeMultiIssuerRequest::new(tokens);

        assert!(request.validate(&None::<Logger>).is_ok());
    }

    #[test]
    fn test_authorize_multi_issuer_request_validation_empty_tokens() {
        let request = AuthorizeMultiIssuerRequest::new(vec![]);

        let result = request.validate(&None::<Logger>);
        assert!(matches!(
            result,
            Err(MultiIssuerValidationError::EmptyTokenArray)
        ));
    }

    #[test]
    fn test_authorize_multi_issuer_request_validation_invalid_token() {
        let tokens = vec![TokenInput::new(
            "access_token".to_string(),
            "invalid-jwt-format".to_string(), // Invalid JWT that will fail parsing
        )];

        let request = AuthorizeMultiIssuerRequest::new(tokens);

        let result = request.validate(&None::<Logger>);
        // The new validation logic continues processing even with invalid tokens
        // and only fails on missing issuer or non-deterministic tokens
        // The token with invalid JWT format should fail at the JWT parsing stage
        // and be added to failed_tokens, but validation should continue
        // Since there are no valid tokens, we should get TokenValidationFailed
        assert!(matches!(
            result,
            Err(MultiIssuerValidationError::TokenValidationFailed { failed_types, total_count })
            if failed_types == vec!["access_token"] && total_count == 1
        ));
    }

    #[test]
    fn test_authorize_multi_issuer_request_validation_invalid_json_fields() {
        let tokens = vec![create_test_token(
            "Jans::Access_Token",
            "https://example.com",
            "1234567890",
        )];

        let request = AuthorizeMultiIssuerRequest::new_with_fields(
            tokens,
            Some(json!(123)), // Invalid resource (should be object or string)
            Some(json!(123)), // Invalid action (should be string)
            Some(json!(123)), // Invalid context (should be object)
        );

        let result = request.validate(&None::<Logger>);
        assert!(matches!(
            result,
            Err(MultiIssuerValidationError::InvalidResourceJson)
        ));
    }

    #[test]
    fn test_serialization_deserialization() {
        let tokens = vec![create_test_token(
            "Jans::Access_Token",
            "https://example.com",
            "1234567890",
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

    #[test]
    fn test_comprehensive_validation_success() {
        let tokens = vec![
            create_test_token("Jans::Access_Token", "https://example.com", "1234567890"),
            create_test_token("Jans::Id_Token", "https://example.com", "1234567890"),
        ];

        let request = AuthorizeMultiIssuerRequest::new(tokens);
        assert!(request.validate(&None::<Logger>).is_ok());
    }

    #[test]
    fn test_non_deterministic_token_detection() {
        let tokens = vec![
            create_test_token("Jans::Access_Token", "https://example.com", "1234567890"),
            create_test_token("Jans::Access_Token", "https://example.com", "9876543210"),
        ];

        let request = AuthorizeMultiIssuerRequest::new(tokens);
        let result = request.validate(&None::<Logger>);
        assert!(matches!(
            result,
            Err(MultiIssuerValidationError::NonDeterministicToken { issuer, token_type })
            if issuer == "https://example.com" && token_type == "Jans::Access_Token"
        ));
    }

    #[test]
    fn test_missing_issuer_claim() {
        let tokens = vec![create_test_token_no_issuer(
            "Jans::Access_Token",
            "1234567890",
        )];

        let request = AuthorizeMultiIssuerRequest::new(tokens);
        let result = request.validate(&None::<Logger>);
        assert!(matches!(
            result,
            Err(MultiIssuerValidationError::MissingIssuer)
        ));
    }

    #[test]
    fn test_flexible_token_type_extraction() {
        // Test standard token types
        let token1 = create_test_token("Jans::Access_Token", "https://example.com", "1234567890");
        let (token_type1, _) = token1.parse_and_validate().unwrap();
        assert_eq!(token_type1, "Jans::Access_Token");

        // Test custom token types from design document
        let token2 = create_test_token("Acme::DolphinToken", "https://example.com", "1234567890");
        let (token_type2, _) = token2.parse_and_validate().unwrap();
        assert_eq!(token_type2, "Acme::DolphinToken");

        // Test another custom token type
        let token3 =
            create_test_token("Custom::EmployeeToken", "https://example.com", "1234567890");
        let (token_type3, _) = token3.parse_and_validate().unwrap();
        assert_eq!(token_type3, "Custom::EmployeeToken");
    }

    #[test]
    fn test_token_type_extraction_with_simple_names() {
        // Test that simple token names work (compatibility with existing system)
        let token = create_test_token("access_token", "https://example.com", "1234567890");
        let result = token.parse_and_validate();
        assert!(result.is_ok());
        let (token_type, _) = result.unwrap();
        assert_eq!(token_type, "access_token");
    }

    #[test]
    fn test_detailed_error_message_for_failed_tokens() {
        // Test that we get detailed error information when all tokens fail validation
        let tokens = vec![
            TokenInput::new(
                "Jans::Access_Token".to_string(),
                "invalid-jwt-1".to_string(),
            ),
            TokenInput::new("Jans::Id_Token".to_string(), "invalid-jwt-2".to_string()),
        ];

        let request = AuthorizeMultiIssuerRequest::new(tokens);
        let result = request.validate(&None::<Logger>);

        match result {
            Err(MultiIssuerValidationError::TokenValidationFailed {
                failed_types,
                total_count,
            }) => {
                assert_eq!(failed_types, vec!["Jans::Access_Token", "Jans::Id_Token"]);
                assert_eq!(total_count, 2);
            },
            _ => panic!("Expected TokenValidationFailed error"),
        }
    }
}
