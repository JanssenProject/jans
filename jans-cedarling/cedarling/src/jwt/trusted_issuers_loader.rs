// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::{collections::HashMap, sync::Arc};

use crate::{
    JwtConfig, LogLevel,
    common::{issuer_utils::IssClaim, policy_store::TrustedIssuer},
    jwt::{
        GetFromUrl, IssuerConfig, IssuerIndex, JwtLogEntry, JwtServiceInitError, KeyService,
        OpenIdConfig, TokenCache, key_service::KeyServiceError, status_list::StatusListCache,
        validation::JwtValidatorCache,
    },
    log::{LogWriter, Logger},
};

/// Loads and initializes trusted issuers for JWT validation.
pub(super) struct TrustedIssuerLoader {
    pub(super) jwt_config: JwtConfig,
    pub(super) status_lists: Arc<StatusListCache>,
    pub(super) issuer_configs: Arc<IssuerIndex>,
    pub(super) validators: Arc<JwtValidatorCache>,
    pub(super) key_service: Arc<KeyService>,
    pub(super) token_cache: TokenCache,
    pub(super) logger: Option<Logger>,
}

impl TrustedIssuerLoader {
    pub(super) async fn load_trusted_issuers(
        &self,
        trusted_issuers: HashMap<String, TrustedIssuer>,
    ) -> Result<(), JwtServiceInitError> {
        for (issuer_id, iss) in trusted_issuers {
            self.load_trusted_issuer(issuer_id, iss).await?;
        }
        Ok(())
    }

    pub(super) async fn load_trusted_issuer(
        &self,
        issuer_id: String,
        iss: TrustedIssuer,
    ) -> Result<(), JwtServiceInitError> {
        // this is what we expect to find in the JWT `iss` claim
        let mut iss_claim = iss.iss_claim();

        let mut iss_config = IssuerConfig {
            issuer_id,
            policy: Arc::new(iss),
            openid_config: None,
        };

        if self.jwt_config.jwt_sig_validation || self.jwt_config.jwt_status_validation {
            iss_claim = update_openid_config(&mut iss_config, &self.logger).await?;
        }

        insert_keys(
            &self.key_service,
            &self.jwt_config,
            &iss_config,
            &self.logger,
        )
        .await?;

        self.validators.init_for_iss(
            &iss_config,
            &self.jwt_config,
            &self.status_lists,
            self.logger.clone(),
        );

        if self.jwt_config.jwt_status_validation {
            self.status_lists
                .init_for_iss(
                    &iss_config,
                    &self.validators,
                    &self.key_service,
                    self.token_cache.clone(),
                    self.logger.clone(),
                )
                .await?;
        }

        self.issuer_configs.insert(iss_claim, iss_config);

        Ok(())
    }

    /// Quick check so we don't get surprised if the program runs but can't validate
    /// if signed authorization is unavailable and signature validation is enabled, log a warning
    pub(super) fn check_keys_loaded(&self) {
        // anything
        let signed_authz_available = self.key_service.has_keys();
        if !signed_authz_available && self.jwt_config.jwt_sig_validation {
            self.logger.log_any(JwtLogEntry::new(
                "signed authorization is unavailable because no trusted issuers or JWKS were configured".to_string(),
                Some(LogLevel::WARN),
            ));
        }
    }
}

async fn update_openid_config(
    iss_config: &mut IssuerConfig,
    logger: &Option<Logger>,
) -> Result<IssClaim, JwtServiceInitError> {
    let openid_config = OpenIdConfig::get_from_url(iss_config.policy.get_oidc_endpoint())
        .await
        .inspect_err(|e| {
            logger.log_any(JwtLogEntry::new(
                format!(
                    "failed to get openid configuration for trusted issuer: '{}': {}",
                    iss_config.issuer_id, e
                ),
                Some(LogLevel::ERROR),
            ));
        })?;

    let iss_claim = openid_config.issuer.clone();
    iss_config.openid_config = Some(openid_config);

    Ok(iss_claim)
}

async fn insert_keys(
    key_service: &KeyService,
    jwt_config: &JwtConfig,
    iss_config: &IssuerConfig,
    logger: &Option<Logger>,
) -> Result<(), KeyServiceError> {
    if !jwt_config.jwt_sig_validation {
        return Ok(());
    }

    if let Some(jwks) = jwt_config.jwks.as_ref() {
        key_service.insert_keys_from_str(jwks)?;
    }

    if let Some(openid_config) = iss_config.openid_config.as_ref() {
        key_service
            .get_keys_using_oidc(openid_config, logger)
            .await?;
    }

    Ok(())
}
