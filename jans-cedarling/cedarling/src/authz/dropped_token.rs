// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Reporting for tokens dropped during multi-issuer authorization.
//!
//! Multi-issuer authorization is tolerant: a token that fails validation or
//! entity building is dropped and the remaining tokens still produce a
//! decision. Historically a drop was only visible in the logs, so a decision
//! made with two of three tokens looked identical to one made with all three.
//!
//! [`DroppedToken`] surfaces each drop on the authorization result and in the
//! decision-log audit trail. It identifies the caller's token entry by its
//! input `mapping` and zero-based `index`, and carries a claim-free
//! [`DropReason`].

use serde::{Deserialize, Serialize};

use super::errors::TokenInputError;

/// A token the caller supplied that multi-issuer authorization did not use.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct DroppedToken {
    /// Cedar entity-type mapping of the dropped input, e.g.
    /// `Jans::Access_Token`. This is the caller-supplied label, never a token
    /// payload or claim value.
    pub mapping: String,
    /// Zero-based position of the token in the request's `tokens` array.
    pub index: usize,
    /// Why the token was dropped, see [`DropReason`].
    pub reason: DropReason,
}

impl DroppedToken {
    /// Construct a [`DroppedToken`] for the entry at `index` with `mapping`.
    #[must_use]
    pub fn new(mapping: impl Into<String>, index: usize, reason: DropReason) -> Self {
        Self {
            mapping: mapping.into(),
            index,
            reason,
        }
    }
}

/// Why a token was dropped from a multi-issuer authorization.
///
/// # Privacy
///
/// `Display` and the serialized form are deliberately claim-free: they never
/// embed token payloads, claim values, or processor-supplied error text. Only
/// the drop category (and, for [`DropReason::InvalidInput`], which input field
/// was malformed) is exposed. The serde representation is adjacently tagged
/// `{"kind": "...", "detail": ..., "message": "..."}`: `detail` only appears
/// for [`DropReason::InvalidInput`], while `message` is the [`DropReason`]
/// `Display` text and is therefore present and non-empty for every variant,
/// including unit ones. The `kind` slugs are a stable API consumed by
/// language bindings; see [`DropReason::slug`].
#[derive(Debug, Clone, PartialEq, Eq, Deserialize, thiserror::Error)]
#[serde(tag = "kind", content = "detail", rename_all = "snake_case")]
pub enum DropReason {
    /// The token input itself was malformed (empty mapping or payload) before
    /// any validation was attempted.
    #[error("token input was invalid: {0}")]
    InvalidInput(TokenInputError),

    /// JWT validation failed, or no trusted issuer declares the token's mapping.
    #[error("JWT validation failed")]
    JwtValidationFailed,

    /// A token with the same issuer and token type was already accepted.
    #[error("duplicate issuer and token-type combination")]
    DuplicateToken,

    /// The mapping routes to a custom issuer but no
    /// [`CustomTokenProcessor`](crate::CustomTokenProcessor) is registered, and
    /// the mapping is not marked required.
    #[error("no custom token processor registered for this mapping")]
    NoProcessorRegistered,

    /// A registered custom processor rejected the token.
    #[error("custom token processing failed")]
    CustomProcessingFailed,

    /// A registered custom processor did not finish within the configured timeout.
    #[error("custom token processing timed out")]
    CustomProcessingTimedOut,

    /// The token validated but a Cedar entity could not be built from it.
    #[error("entity building failed")]
    EntityBuildFailed,
}

impl Serialize for DropReason {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
        use serde::ser::SerializeMap;

        let mut map = serializer.serialize_map(None)?;
        map.serialize_entry("kind", self.slug())?;
        if let Some(detail) = self.detail() {
            map.serialize_entry("detail", detail)?;
        }
        map.serialize_entry("message", &self.message())?;
        map.end()
    }
}

impl DropReason {
    /// Stable slug identifying this reason which matches the serde `kind` tag and
    /// is safe to use as a metric label, a structured log field, or a
    /// discriminant in a language binding.
    #[must_use]
    pub const fn slug(&self) -> &'static str {
        match self {
            Self::InvalidInput(_) => "invalid_input",
            Self::JwtValidationFailed => "jwt_validation_failed",
            Self::DuplicateToken => "duplicate_token",
            Self::NoProcessorRegistered => "no_processor_registered",
            Self::CustomProcessingFailed => "custom_processing_failed",
            Self::CustomProcessingTimedOut => "custom_processing_timed_out",
            Self::EntityBuildFailed => "entity_build_failed",
        }
    }

    /// Claim-free human-readable message for this reason, derived from
    /// [`Display`](std::fmt::Display). Always non-empty, including for unit
    /// variants. Serialized as the `message` entry and surfaced through the
    /// result, decision log, and language bindings.
    #[must_use]
    pub fn message(&self) -> String {
        self.to_string()
    }

    /// Stable machine-readable detail for this reason. Currently `Some` only
    /// for [`DropReason::InvalidInput`], where it names the malformed input
    /// field (`"empty_mapping"` / `"empty_payload"`); `None` otherwise.
    /// Bindings surface this as the flattened `detail` string (empty when
    /// `None`) so every language reports the same value as the core JSON.
    #[must_use]
    pub const fn detail(&self) -> Option<&'static str> {
        match self {
            Self::InvalidInput(err) => Some(match err {
                TokenInputError::EmptyMapping => "empty_mapping",
                TokenInputError::EmptyPayload => "empty_payload",
            }),
            _ => None,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn slug_matches_serde_kind_tag() {
        let cases = [
            DropReason::InvalidInput(TokenInputError::EmptyMapping),
            DropReason::JwtValidationFailed,
            DropReason::DuplicateToken,
            DropReason::NoProcessorRegistered,
            DropReason::CustomProcessingFailed,
            DropReason::CustomProcessingTimedOut,
            DropReason::EntityBuildFailed,
        ];
        for reason in cases {
            let value = serde_json::to_value(&reason).expect("serialize reason");
            let kind = value
                .get("kind")
                .and_then(|k| k.as_str())
                .expect("reason must serialize a `kind` tag");
            assert_eq!(
                kind,
                reason.slug(),
                "slug() must match serde kind for {reason:?}"
            );
        }
    }

    #[test]
    fn invalid_input_carries_claim_free_detail() {
        let reason = DropReason::InvalidInput(TokenInputError::EmptyPayload);
        let value = serde_json::to_value(&reason).expect("serialize");
        assert_eq!(
            value,
            json!({
                "kind": "invalid_input",
                "detail": "empty_payload",
                "message": "token input was invalid: Empty payload"
            }),
            "InvalidInput should carry the specific input error as adjacent detail"
        );
    }

    #[test]
    fn every_reason_serializes_non_empty_message() {
        let cases = [
            DropReason::InvalidInput(TokenInputError::EmptyMapping),
            DropReason::JwtValidationFailed,
            DropReason::DuplicateToken,
            DropReason::NoProcessorRegistered,
            DropReason::CustomProcessingFailed,
            DropReason::CustomProcessingTimedOut,
            DropReason::EntityBuildFailed,
        ];
        for reason in cases {
            let value = serde_json::to_value(&reason).expect("serialize reason");
            let message = value
                .get("message")
                .and_then(|m| m.as_str())
                .unwrap_or_default();
            assert_eq!(
                message,
                reason.to_string(),
                "message must be the claim-free Display text for {reason:?}"
            );
            assert!(
                !message.is_empty(),
                "message must be non-empty for every reason, got {reason:?}"
            );
        }
    }

    #[test]
    fn unit_reason_omits_detail() {
        let value = serde_json::to_value(DropReason::EntityBuildFailed).expect("serialize");
        assert_eq!(
            value,
            json!({
                "kind": "entity_build_failed",
                "message": "entity building failed"
            }),
            "unit reasons must not emit a detail field but must carry a message"
        );
    }

    #[test]
    fn dropped_token_round_trips_json() {
        let dropped = DroppedToken::new("Jans::Access_Token", 2, DropReason::JwtValidationFailed);
        let s = serde_json::to_string(&dropped).expect("serialize");
        let round: DroppedToken = serde_json::from_str(&s).expect("deserialize");
        assert_eq!(
            dropped, round,
            "DroppedToken should round-trip through JSON"
        );

        // The hand-written `message` entry must not disturb the derived
        // `Deserialize`, which only reads `kind` and `detail`.
        let dropped = DroppedToken::new(
            "Jans::Access_Token",
            0,
            DropReason::InvalidInput(TokenInputError::EmptyPayload),
        );
        let s = serde_json::to_string(&dropped).expect("serialize");
        let round: DroppedToken = serde_json::from_str(&s).expect("deserialize");
        assert_eq!(
            dropped, round,
            "an InvalidInput DroppedToken should round-trip through JSON"
        );
    }

    #[test]
    fn display_is_claim_free() {
        // None of the Display strings may contain claim values; they are fixed
        // category text plus (for InvalidInput) the input-field error.
        assert_eq!(
            DropReason::InvalidInput(TokenInputError::EmptyMapping).to_string(),
            "token input was invalid: Empty mapping string"
        );
        assert_eq!(
            DropReason::CustomProcessingTimedOut.to_string(),
            "custom token processing timed out"
        );
    }
}
