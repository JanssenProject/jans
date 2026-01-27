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
    jwt_config::TrustedIssuerLoaderConfig,
    log::{BaseLogEntry, LogEntry, LogWriter, Logger},
};

use crate::http::spawn_task;

/// Loads and initializes trusted issuers for JWT validation.
#[derive(Clone)]
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
    /// Load trusted issuers either synchronously or asynchronously based on configuration.
    pub(super) async fn load_trusted_issuers(
        &self,
        trusted_issuers: HashMap<String, TrustedIssuer>,
    ) -> Result<(), JwtServiceInitError> {
        match self.jwt_config.trusted_issuer_loader {
            TrustedIssuerLoaderConfig::Sync => {
                for (issuer_id, iss) in trusted_issuers {
                    load_trusted_issuer(self, issuer_id, iss).await?;
                }
                self.check_keys_loaded();
            },
            TrustedIssuerLoaderConfig::Async { workers } => {
                self.load_trusted_issuer_async(trusted_issuers, workers.get())
                    .await
            },
        }

        Ok(())
    }

    /// Load trusted issuers asynchronously with a limit on concurrent workers.
    async fn load_trusted_issuer_async(
        &self,
        trusted_issuers: HashMap<String, TrustedIssuer>,
        workers: usize,
    ) {
        let semaphore = Arc::new(tokio::sync::Semaphore::new(workers));
        let mut handles = Vec::new();

        for (issuer_id, iss) in trusted_issuers {
            let permit = semaphore.clone().acquire_owned().await.unwrap();
            let loader_clone = self.clone();

            let handle = spawn_task(async move {
                let result = load_trusted_issuer(&loader_clone, issuer_id.clone(), iss).await;
                drop(permit); // Release the permit

                if let Err(error) = result {
                    // log error but without failing the entire load process
                    loader_clone.logger.log_any(
                        LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                            LogLevel::WARN,
                            None,
                        ))
                        .set_message(format!("Could not load trusted issuer: {}", issuer_id))
                        .set_error(error.to_string()),
                    );
                }
            });
            handles.push(handle);
        }

        // Await all tasks to complete
        for handle in handles {
            handle.await_result().await;
        }

        self.check_keys_loaded();
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

pub(super) async fn load_trusted_issuer(
    loader: &TrustedIssuerLoader,
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

    if loader.jwt_config.jwt_sig_validation || loader.jwt_config.jwt_status_validation {
        iss_claim = update_openid_config(&mut iss_config, &loader.logger).await?;
    }

    insert_keys(
        &loader.key_service,
        &loader.jwt_config,
        &iss_config,
        &loader.logger,
    )
    .await?;

    loader.validators.init_for_iss(
        &iss_config,
        &loader.jwt_config,
        &loader.status_lists,
        loader.logger.clone(),
    );

    if loader.jwt_config.jwt_status_validation {
        loader
            .status_lists
            .init_for_iss(
                &iss_config,
                &loader.validators,
                &loader.key_service,
                loader.token_cache.clone(),
                loader.logger.clone(),
            )
            .await?;
    }

    loader.issuer_configs.insert(iss_claim, iss_config);

    Ok(())
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
