// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::{
    collections::HashMap,
    sync::{Arc, Mutex},
};
use thiserror::Error;

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

#[derive(Error, Debug)]
pub enum TrustedIssuerLoaderError {
    #[error(
        "failed to acquire semaphore permit for concurrent issuer loading - this indicates a serious concurrency issue or resource exhaustion"
    )]
    SemaphoreAcquire,
    #[error(
        "failed to extract errors Arc - other references may still exist indicating concurrent access during issuer loading"
    )]
    ErrorsArcExtraction,
    #[error(
        "failed to extract errors from Mutex - mutex may be poisoned due to panic while holding lock"
    )]
    ErrorsMutexExtraction,
}

/// Loads and initializes trusted issuers for JWT validation.
#[derive(Clone)]
pub(super) struct TrustedIssuerLoader {
    pub(super) jwt_config: JwtConfig,
    pub(super) status_lists: StatusListCache,
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
            TrustedIssuerLoaderConfig::Sync { workers } => {
                load_trusted_issuers(self, trusted_issuers, workers.get())
                    .await
                    .inspect_err(|e| {
                        log_load_trusted_issuers_error(self.logger.as_ref(), e);
                    })
            },
            TrustedIssuerLoaderConfig::Async { workers } => {
                let loader = self.clone();
                spawn_task(async move {
                    let _ = load_trusted_issuers(&loader, trusted_issuers, workers.get())
                        .await
                        .inspect_err(|e| {
                            log_load_trusted_issuers_error(loader.logger.as_ref(), e);
                        });
                });
                Ok(())
            },
        }
    }

    /// Quick check so we don't get surprised if the program runs but can't validate
    /// if signed authorization is unavailable and signature validation is enabled, log a warning
    pub(crate) fn check_keys_loaded(&self) {
        let signed_authz_available = self.key_service.has_keys();
        if !signed_authz_available && self.jwt_config.jwt_sig_validation {
            self.logger.log_any(JwtLogEntry::new(
                "signed authorization is unavailable because no trusted issuers or JWKS were configured".to_string(),
                Some(LogLevel::WARN),
            ));
        }
    }
}

async fn load_trusted_issuers(
    loader: &TrustedIssuerLoader,
    trusted_issuers: HashMap<String, TrustedIssuer>,
    workers: usize,
) -> Result<(), JwtServiceInitError> {
    let semaphore = Arc::new(tokio::sync::Semaphore::new(workers));
    let mut handles = Vec::new();
    let errors = Arc::new(Mutex::new(Vec::new()));

    for (issuer_id, iss) in trusted_issuers {
        let permit = semaphore
            .clone()
            .acquire_owned()
            .await
            .map_err(|_| TrustedIssuerLoaderError::SemaphoreAcquire)?;
        let loader_clone = loader.clone();
        let errors_clone = errors.clone();

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
                    .set_message(format!("Could not load trusted issuer: {issuer_id}"))
                    .set_error(error.to_string()),
                );

                // we can't return the error from this async task
                // so we can use only `expect`
                errors_clone
                    .lock()
                    .expect("failed to lock errors mutex while recording issuer loading failure - mutex may be poisoned due to panic in another thread")
                    .push(error);
            }
        });
        handles.push(handle);
    }

    // Await all tasks to complete
    for handle in handles {
        handle.await_result().await;
    }

    loader.check_keys_loaded();

    let errors = Arc::into_inner(errors)
        .ok_or(TrustedIssuerLoaderError::ErrorsArcExtraction)?
        .into_inner()
        .map_err(|_| TrustedIssuerLoaderError::ErrorsMutexExtraction)?;

    // if there were any errors, return the first one
    if let Some(first_error) = errors.into_iter().next() {
        return Err(first_error);
    }

    Ok(())
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
        iss_claim = update_openid_config(&mut iss_config, loader.logger.as_ref()).await?;
    }

    insert_keys(
        &loader.key_service,
        &loader.jwt_config,
        &iss_config,
        loader.logger.as_ref(),
    )
    .await?;

    loader.validators.init_for_iss(
        &iss_config,
        &loader.jwt_config,
        &loader.status_lists,
        loader.logger.as_ref(),
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

/// Fetches the `OpenID` configuration for a trusted issuer and updates the issuer configuration accordingly.
async fn update_openid_config(
    iss_config: &mut IssuerConfig,
    logger: Option<&Logger>,
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

/// Inserts keys into the key service based on the JWT configuration and issuer configuration.
async fn insert_keys(
    key_service: &KeyService,
    jwt_config: &JwtConfig,
    iss_config: &IssuerConfig,
    logger: Option<&Logger>,
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

/// Logs a critical error that occurred during the loading of trusted issuers.
fn log_load_trusted_issuers_error(logger: Option<&Logger>, error: &JwtServiceInitError) {
    logger.log_any(
        LogEntry::new(BaseLogEntry::new_system_opt_request_id(
            LogLevel::FATAL,
            None,
        ))
        .set_error(error.to_string())
        .set_message("Error happened on load_trusted_issuers, it is critical".to_string()),
    );
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::jwt::key_service::DecodingKeyInfo;
    use crate::jwt::test_utils::MockServer;
    use crate::{common::issuer_utils::IssClaim, jwt_config::WorkersCount};
    use jsonwebtoken::Algorithm;
    use mockito::Server;
    use std::collections::HashMap;
    use std::sync::Arc;
    use tokio::time::{Duration, sleep};
    use url::Url;

    /// Helper to retry an assertion for a short period, useful for async tests
    /// where operations may complete slightly after tasks are awaited.
    /// First check is performed immediately.
    async fn retry_assert<F, Fut>(mut assertion: F, max_retries: u32, delay_ms: u64, message: &str)
    where
        F: FnMut() -> Fut,
        Fut: std::future::Future<Output = bool>,
    {
        for i in 0..max_retries {
            if i != 0 && i < max_retries - 1 {
                sleep(Duration::from_millis(delay_ms)).await;
            }
            if assertion().await {
                return;
            }
        }
        panic!("{message} (failed after {max_retries} retries)");
    }

    /// Tests synchronous loading of a single trusted issuer.
    ///
    /// Verifies that a trusted issuer can be loaded successfully in sync mode
    /// and that the issuer configuration is properly inserted into the index.
    #[tokio::test]
    async fn load_single_trusted_issuer_sync() {
        let server = MockServer::new_with_defaults().await.unwrap();
        let trusted_issuer = server.trusted_issuer();

        let jwt_config = JwtConfig {
            jwt_sig_validation: false,
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::default(),
            ..Default::default()
        };

        let loader = TrustedIssuerLoader {
            jwt_config,
            status_lists: StatusListCache::default(),
            issuer_configs: Arc::new(IssuerIndex::new()),
            validators: Arc::new(JwtValidatorCache::default()),
            key_service: Arc::new(KeyService::new()),
            token_cache: TokenCache::default(),
            logger: None,
        };

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("test_issuer".to_string(), trusted_issuer);

        let result = loader.load_trusted_issuers(trusted_issuers).await;
        result.expect("should load single trusted issuer successfully in sync mode");

        // Verify issuer config was inserted
        let issuer_claim = server.issuer();
        let trusted_issuer_from_index = loader.issuer_configs.get_trusted_issuer(&issuer_claim);
        assert!(
            trusted_issuer_from_index.is_some(),
            "trusted issuer should be inserted into issuer index"
        );

        // Verify no keys are stored when signature validation is disabled
        assert!(
            !loader.key_service.has_keys(),
            "key service should have no keys when signature validation is disabled"
        );
    }

    /// Tests asynchronous loading of multiple trusted issuers.
    ///
    /// Verifies that multiple issuers can be loaded concurrently with a worker limit
    /// and that all issuer configurations are properly inserted into the index.
    #[tokio::test]
    async fn load_multiple_trusted_issuers_async() {
        // Create two mock servers
        let server1 = MockServer::new_with_defaults().await.unwrap();
        let server2 = MockServer::new_with_defaults().await.unwrap();

        let trusted_issuer_1 = server1.trusted_issuer();
        let trusted_issuer_2 = server2.trusted_issuer();

        let jwt_config = JwtConfig {
            jwt_sig_validation: false,
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::Async {
                workers: WorkersCount::new(2),
            },
            ..Default::default()
        };

        let loader = TrustedIssuerLoader {
            jwt_config,
            status_lists: StatusListCache::default(),
            issuer_configs: Arc::new(IssuerIndex::new()),
            validators: Arc::new(JwtValidatorCache::default()),
            key_service: Arc::new(KeyService::new()),
            token_cache: TokenCache::default(),
            logger: None,
        };

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("issuer1".to_string(), trusted_issuer_1);
        trusted_issuers.insert("issuer2".to_string(), trusted_issuer_2);

        let result = loader.load_trusted_issuers(trusted_issuers).await;
        result.expect("should load multiple trusted issuers successfully in async mode");

        // Verify both issuers were loaded with retry for async operations
        let issuer_claim1 = server1.issuer();
        let issuer_claim2 = server2.issuer();
        retry_assert(
            || async {
                loader
                    .issuer_configs
                    .get_trusted_issuer(&issuer_claim1)
                    .is_some()
            },
            10,
            10,
            "first trusted issuer should be inserted into issuer index",
        )
        .await;
        retry_assert(
            || async {
                loader
                    .issuer_configs
                    .get_trusted_issuer(&issuer_claim2)
                    .is_some()
            },
            10,
            10,
            "second trusted issuer should be inserted into issuer index",
        )
        .await;

        // Verify no keys are stored when signature validation is disabled
        assert!(
            !loader.key_service.has_keys(),
            "key service should have no keys when signature validation is disabled"
        );
    }

    /// Tests synchronous loading when `OpenID` configuration fetch fails.
    ///
    /// Verifies that sync mode returns an error when the `OpenID` endpoint
    /// is unreachable and signature validation is enabled.
    #[tokio::test]
    async fn load_trusted_issuer_with_failing_openid_config_sync() {
        // Create a mock server but don't set up the OIDC endpoint
        let server = Server::new_async().await;
        // No mocks created, so request will fail

        let mut trusted_issuer = TrustedIssuer::default();
        trusted_issuer.set_oidc_endpoint(
            Url::parse(&(server.url() + "/.well-known/openid-configuration")).unwrap(),
        );

        let jwt_config = JwtConfig {
            jwt_sig_validation: true, // Requires OpenID config
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::default(),
            ..Default::default()
        };

        let loader = TrustedIssuerLoader {
            jwt_config,
            status_lists: StatusListCache::default(),
            issuer_configs: Arc::new(IssuerIndex::new()),
            validators: Arc::new(JwtValidatorCache::default()),
            key_service: Arc::new(KeyService::new()),
            token_cache: TokenCache::default(),
            logger: None,
        };

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("failing_issuer".to_string(), trusted_issuer);

        let result = loader.load_trusted_issuers(trusted_issuers).await;
        result.expect_err(
            "should fail to load trusted issuer when OpenID endpoint is unreachable in sync mode",
        );
    }

    /// Tests asynchronous loading when `OpenID` configuration fetch fails.
    ///
    /// Verifies that async mode logs errors but does not fail the entire load process
    /// when individual issuers cannot be loaded.
    #[tokio::test]
    async fn load_trusted_issuer_with_failing_openid_config_async() {
        // Create a mock server but don't set up the OIDC endpoint
        let server = Server::new_async().await;

        let mut trusted_issuer = TrustedIssuer::default();
        trusted_issuer.set_oidc_endpoint(
            Url::parse(&(server.url() + "/.well-known/openid-configuration")).unwrap(),
        );

        let jwt_config = JwtConfig {
            jwt_sig_validation: true,
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::Async {
                workers: WorkersCount::new(1),
            },
            ..Default::default()
        };

        let loader = TrustedIssuerLoader {
            jwt_config,
            status_lists: StatusListCache::default(),
            issuer_configs: Arc::new(IssuerIndex::new()),
            validators: Arc::new(JwtValidatorCache::default()),
            key_service: Arc::new(KeyService::new()),
            token_cache: TokenCache::default(),
            logger: None,
        };

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("failing_issuer".to_string(), trusted_issuer);

        let result = loader.load_trusted_issuers(trusted_issuers).await;
        // Async mode should not return error for individual failures
        result.expect("async mode should not fail entire load process when individual issuers cannot be loaded");
    }

    /// Tests the warning check for missing keys when signature validation is enabled.
    ///
    /// Verifies that `check_keys_loaded` does not panic and handles both empty
    /// and populated key service states correctly.
    #[test]
    fn check_keys_loaded_warning() {
        // Test that warning is logged when jwt_sig_validation is enabled but no keys loaded
        let jwt_config = JwtConfig {
            jwt_sig_validation: true,
            jwt_status_validation: false,
            ..Default::default()
        };

        let loader = TrustedIssuerLoader {
            jwt_config,
            status_lists: StatusListCache::default(),
            issuer_configs: Arc::new(IssuerIndex::new()),
            validators: Arc::new(JwtValidatorCache::default()),
            key_service: Arc::new(KeyService::new()), // empty key service
            token_cache: TokenCache::default(),
            logger: None,
        };

        // This should not panic
        loader.check_keys_loaded();

        // Now test with keys present
        let key_service_with_keys = KeyService::new();
        // Insert a dummy key (invalid but enough for has_keys check)
        let jwks =
            r#"{"test_issuer":[{"kty":"RSA","kid":"test","alg":"RS256","e":"AQAB","n":"test"}]}"#;
        key_service_with_keys.insert_keys_from_str(jwks).unwrap();

        let loader_with_keys = TrustedIssuerLoader {
            jwt_config: JwtConfig {
                jwt_sig_validation: true,
                jwt_status_validation: false,
                ..Default::default()
            },
            status_lists: StatusListCache::default(),
            issuer_configs: Arc::new(IssuerIndex::new()),
            validators: Arc::new(JwtValidatorCache::default()),
            key_service: Arc::new(key_service_with_keys),
            token_cache: TokenCache::default(),
            logger: None,
        };

        loader_with_keys.check_keys_loaded();
    }

    /// Tests loading trusted issuers with JWKS provided in configuration.
    ///
    /// Verifies that JWKS from the configuration are properly inserted into
    /// the key service when signature validation is enabled.
    #[tokio::test]
    async fn load_with_jwks_config() {
        let server = MockServer::new_with_defaults().await.unwrap();
        let trusted_issuer = server.trusted_issuer();

        // Provide JWKS directly in config (invalid key but parsing will succeed)
        let jwks =
            r#"{"test_issuer":[{"kty":"RSA","kid":"test","alg":"RS256","e":"AQAB","n":"test"}]}"#;

        let jwt_config = JwtConfig {
            jwks: Some(jwks.to_string()),
            jwt_sig_validation: true,
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::default(),
            ..Default::default()
        };

        let loader = TrustedIssuerLoader {
            jwt_config,
            status_lists: StatusListCache::default(),
            issuer_configs: Arc::new(IssuerIndex::new()),
            validators: Arc::new(JwtValidatorCache::default()),
            key_service: Arc::new(KeyService::new()),
            token_cache: TokenCache::default(),
            logger: None,
        };

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("test_issuer".to_string(), trusted_issuer);

        let result = loader.load_trusted_issuers(trusted_issuers).await;
        result.expect(
            "should load trusted issuer successfully when JWKS is provided in configuration",
        );

        // Key service should have keys
        assert!(
            loader.key_service.has_keys(),
            "key service should have keys after loading JWKS from configuration"
        );

        // Verify the JWKS key from configuration is stored with correct issuer, kid, and algorithm
        let jwks_key_info = DecodingKeyInfo {
            issuer: Some(IssClaim::new("test_issuer")),
            kid: Some("test".to_string()),
            algorithm: Algorithm::RS256,
        };
        assert!(
            loader.key_service.get_key(&jwks_key_info).is_some(),
            "JWKS key from configuration should be stored in key service"
        );

        // Verify the OpenID config key from mock server is also stored (HS256)
        let openid_key_info = DecodingKeyInfo {
            issuer: Some(server.issuer()),
            kid: Some("some_hs256_key".to_string()),
            algorithm: Algorithm::HS256,
        };
        assert!(
            loader.key_service.get_key(&openid_key_info).is_some(),
            "OpenID config key from mock server should be stored in key service"
        );
    }

    /// Tests loading a trusted issuer with signature validation enabled.
    ///
    /// Verifies that when `jwt_sig_validation` is true, keys from `OpenID` configuration
    /// are properly fetched and stored in the key service.
    #[tokio::test]
    async fn load_trusted_issuer_with_signature_validation() {
        let server = MockServer::new_with_defaults().await.unwrap();
        let trusted_issuer = server.trusted_issuer();

        let jwt_config = JwtConfig {
            jwt_sig_validation: true,
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::default(),
            ..Default::default()
        };

        let loader = TrustedIssuerLoader {
            jwt_config,
            status_lists: StatusListCache::default(),
            issuer_configs: Arc::new(IssuerIndex::new()),
            validators: Arc::new(JwtValidatorCache::default()),
            key_service: Arc::new(KeyService::new()),
            token_cache: TokenCache::default(),
            logger: None,
        };

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("test_issuer".to_string(), trusted_issuer);

        let result = loader.load_trusted_issuers(trusted_issuers).await;
        result.expect("should load trusted issuer successfully with signature validation enabled");

        // Verify issuer config was inserted
        let issuer_claim = server.issuer();
        let trusted_issuer_from_index = loader.issuer_configs.get_trusted_issuer(&issuer_claim);
        assert!(
            trusted_issuer_from_index.is_some(),
            "trusted issuer should be inserted into issuer index"
        );

        // Verify key service has keys and specific HS256 key is stored
        assert!(
            loader.key_service.has_keys(),
            "key service should have keys after loading OpenID configuration"
        );

        let key_info = DecodingKeyInfo {
            issuer: Some(server.issuer()),
            kid: Some("some_hs256_key".to_string()),
            algorithm: Algorithm::HS256,
        };
        assert!(
            loader.key_service.get_key(&key_info).is_some(),
            "HS256 key from OpenID configuration should be stored in key service"
        );
    }

    /// Tests synchronous loading of a single trusted issuer with status validation enabled.
    ///
    /// Verifies that when `jwt_status_validation` is true, the status list cache is initialized
    /// for the issuer and contains the expected status list.
    #[tokio::test]
    async fn load_trusted_issuer_with_status_validation_sync() {
        let mut server = MockServer::new_with_defaults().await.unwrap();
        // Generate status list endpoint before creating trusted issuer
        // First consume the default OIDC mock to allow clean replacement
        let client = reqwest::Client::new();
        let oidc_url = server.openid_config_endpoint().unwrap();
        let _ = client.get(oidc_url.as_str()).send().await.unwrap();

        // Now generate status list endpoint and update OIDC config
        server.generate_status_list_endpoint(1u8.try_into().unwrap(), &[0b1111_1110], None);
        server.update_openid_config_with_status_list_endpoint();
        let trusted_issuer = server.trusted_issuer();

        let jwt_config = JwtConfig {
            jwt_sig_validation: true,
            jwt_status_validation: true,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::default(),
            signature_algorithms_supported: std::collections::HashSet::from([Algorithm::HS256]),
            ..Default::default()
        };

        let loader = TrustedIssuerLoader {
            jwt_config,
            status_lists: StatusListCache::default(),
            issuer_configs: Arc::new(IssuerIndex::new()),
            validators: Arc::new(JwtValidatorCache::default()),
            key_service: Arc::new(KeyService::new()),
            token_cache: TokenCache::default(),
            logger: None,
        };

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("test_issuer".to_string(), trusted_issuer);

        let result = loader.load_trusted_issuers(trusted_issuers).await;
        result.expect("should load trusted issuer successfully with status validation enabled");

        // Verify issuer config was inserted
        let issuer_claim = server.issuer();
        let trusted_issuer_from_index = loader.issuer_configs.get_trusted_issuer(&issuer_claim);
        assert!(
            trusted_issuer_from_index.is_some(),
            "trusted issuer should be inserted into issuer index"
        );

        // Verify status list cache contains the endpoint
        let status_list_uri = server.status_list_endpoint().unwrap().to_string();
        let status_lists = loader.status_lists.status_lists.read().unwrap();
        assert!(
            status_lists.contains_key(&status_list_uri),
            "status list cache should contain the status list endpoint"
        );
    }

    /// Tests asynchronous loading of multiple trusted issuers with signature validation enabled.
    ///
    /// Verifies that when `jwt_sig_validation` is true and async loading is used,
    /// keys from `OpenID` configurations are properly fetched and stored in the key service.
    #[tokio::test]
    async fn load_multiple_trusted_issuers_async_with_signature_validation() {
        // Create two mock servers
        let server1 = MockServer::new_with_defaults().await.unwrap();
        let server2 = MockServer::new_with_defaults().await.unwrap();

        let trusted_issuer_1 = server1.trusted_issuer();
        let trusted_issuer_2 = server2.trusted_issuer();

        let jwt_config = JwtConfig {
            jwt_sig_validation: true,
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::Async {
                workers: WorkersCount::new(2),
            },
            ..Default::default()
        };

        let loader = TrustedIssuerLoader {
            jwt_config,
            status_lists: StatusListCache::default(),
            issuer_configs: Arc::new(IssuerIndex::new()),
            validators: Arc::new(JwtValidatorCache::default()),
            key_service: Arc::new(KeyService::new()),
            token_cache: TokenCache::default(),
            logger: None,
        };

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("issuer1".to_string(), trusted_issuer_1);
        trusted_issuers.insert("issuer2".to_string(), trusted_issuer_2);

        let result = loader.load_trusted_issuers(trusted_issuers).await;
        result.expect("should load multiple trusted issuers successfully in async mode with signature validation");

        // Verify both issuers were loaded with retry for async operations
        let issuer_claim1 = server1.issuer();
        let issuer_claim2 = server2.issuer();
        retry_assert(
            || async {
                loader
                    .issuer_configs
                    .get_trusted_issuer(&issuer_claim1)
                    .is_some()
            },
            10,
            10,
            "first trusted issuer should be inserted into issuer index",
        )
        .await;
        retry_assert(
            || async {
                loader
                    .issuer_configs
                    .get_trusted_issuer(&issuer_claim2)
                    .is_some()
            },
            10,
            10,
            "second trusted issuer should be inserted into issuer index",
        )
        .await;

        // Verify keys are stored for both issuers with retry for async operations
        retry_assert(
            || async { loader.key_service.has_keys() },
            10,
            10,
            "key service should have keys after loading OpenID configurations in async mode",
        )
        .await;

        // Verify specific HS256 keys from both mock servers are stored with retry
        let key_info1 = DecodingKeyInfo {
            issuer: Some(server1.issuer()),
            kid: Some("some_hs256_key".to_string()),
            algorithm: Algorithm::HS256,
        };
        retry_assert(
            || async { loader.key_service.get_key(&key_info1).is_some() },
            10,
            10,
            "HS256 key from first OpenID configuration should be stored in key service",
        )
        .await;

        let key_info2 = DecodingKeyInfo {
            issuer: Some(server2.issuer()),
            kid: Some("some_hs256_key".to_string()),
            algorithm: Algorithm::HS256,
        };
        retry_assert(
            || async { loader.key_service.get_key(&key_info2).is_some() },
            10,
            10,
            "HS256 key from second OpenID configuration should be stored in key service",
        )
        .await;
    }

    /// Tests loading trusted issuers with the many allowed workers.
    ///
    /// Verifies that the loader can handle concurrent loading with the a lot of
    /// workers without panicking or deadlocking.
    #[tokio::test]
    async fn load_max_workers_concurrent() {
        // Create multiple mock servers (up to a reasonable count)
        const NUM_ISSUERS: usize = 10;
        let mut servers = Vec::new();
        for _ in 0..NUM_ISSUERS {
            servers.push(MockServer::new_with_defaults().await.unwrap());
        }

        let jwt_config = JwtConfig {
            jwt_sig_validation: true,
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::Async {
                workers: WorkersCount::new(NUM_ISSUERS),
            },
            ..Default::default()
        };

        let loader = TrustedIssuerLoader {
            jwt_config,
            status_lists: StatusListCache::default(),
            issuer_configs: Arc::new(IssuerIndex::new()),
            validators: Arc::new(JwtValidatorCache::default()),
            key_service: Arc::new(KeyService::new()),
            token_cache: TokenCache::default(),
            logger: None,
        };

        let mut trusted_issuers = HashMap::new();
        for (i, server) in servers.iter().enumerate() {
            trusted_issuers.insert(format!("issuer{i}"), server.trusted_issuer());
        }

        let result = loader.load_trusted_issuers(trusted_issuers).await;
        result.expect("should load multiple trusted issuers successfully with max workers");

        // Verify all issuers were loaded with retry for async operations
        for (i, server) in servers.iter().enumerate() {
            let issuer_claim = server.issuer();
            retry_assert(
                || async {
                    loader
                        .issuer_configs
                        .get_trusted_issuer(&issuer_claim)
                        .is_some()
                },
                10,
                1,
                &format!("trusted issuer {i} should be inserted into issuer index"),
            )
            .await;
        }

        // Verify keys are stored when signature validation is enabled
        assert!(
            loader.key_service.has_keys(),
            "key service should have keys when signature validation is enabled"
        );
    }

    /// Tests loading many trusted issuers with a single worker.
    ///
    /// Verifies that the loader can handle many issuers sequentially with a
    /// single worker without panicking or deadlocking.
    #[tokio::test]
    async fn load_single_worker_many_issuers() {
        // Create multiple mock servers
        const NUM_ISSUERS: usize = 5;
        let mut servers = Vec::new();
        for _ in 0..NUM_ISSUERS {
            servers.push(MockServer::new_with_defaults().await.unwrap());
        }

        let jwt_config = JwtConfig {
            jwt_sig_validation: false,
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::Sync {
                workers: WorkersCount::new(1),
            },
            ..Default::default()
        };

        let loader = TrustedIssuerLoader {
            jwt_config,
            status_lists: StatusListCache::default(),
            issuer_configs: Arc::new(IssuerIndex::new()),
            validators: Arc::new(JwtValidatorCache::default()),
            key_service: Arc::new(KeyService::new()),
            token_cache: TokenCache::default(),
            logger: None,
        };

        let mut trusted_issuers = HashMap::new();
        for (i, server) in servers.iter().enumerate() {
            trusted_issuers.insert(format!("issuer{i}"), server.trusted_issuer());
        }

        let result = loader.load_trusted_issuers(trusted_issuers).await;
        result.expect("should load many trusted issuers successfully with single worker");

        // Verify all issuers were loaded
        for (i, server) in servers.iter().enumerate() {
            let issuer_claim = server.issuer();
            assert!(
                loader
                    .issuer_configs
                    .get_trusted_issuer(&issuer_claim)
                    .is_some(),
                "trusted issuer {i} should be inserted into issuer index"
            );
        }

        // Verify no keys are stored when signature validation is disabled
        assert!(
            !loader.key_service.has_keys(),
            "key service should have no keys when signature validation is disabled"
        );
    }

    /// Tests that async loading returns immediately without blocking startup.
    ///
    /// Verifies that when using async loading mode, `load_trusted_issuers` returns
    /// quickly (within a short timeout) while issuer loading continues in the background.
    #[tokio::test]
    async fn async_loading_returns_immediately() {
        let server = MockServer::new_with_defaults().await.unwrap();
        let trusted_issuer = server.trusted_issuer();

        let jwt_config = JwtConfig {
            jwt_sig_validation: false,
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::Async {
                workers: WorkersCount::new(1),
            },
            ..Default::default()
        };

        let loader = TrustedIssuerLoader {
            jwt_config,
            status_lists: StatusListCache::default(),
            issuer_configs: Arc::new(IssuerIndex::new()),
            validators: Arc::new(JwtValidatorCache::default()),
            key_service: Arc::new(KeyService::new()),
            token_cache: TokenCache::default(),
            logger: None,
        };

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("test_issuer".to_string(), trusted_issuer);

        // Start timing
        let start = tokio::time::Instant::now();

        // in `impl GetFromUrl<OpenIdConfig> for OpenIdConfig{`
        // we have 5ms delay (for tests), so if initialization is sync it should take at least 5ms.

        let result = loader.load_trusted_issuers(trusted_issuers).await;
        result.expect("async loading should return Ok immediately");

        let expected_start_delay = Duration::from_millis(1);

        // Verify the function returned quickly (should be < 1ms for async mode)
        let elapsed = tokio::time::Instant::now().duration_since(start);
        assert!(
            elapsed < expected_start_delay,
            "Async loading should return quickly (took {}ms), indicating non-blocking behavior",
            elapsed.as_millis()
        );

        // Verify issuer is not necessarily loaded immediately (async background)
        // but eventually loads within a reasonable time
        let issuer_claim = server.issuer();
        retry_assert(
            || async {
                loader
                    .issuer_configs
                    .get_trusted_issuer(&issuer_claim)
                    .is_some()
            },
            10,
            5,
            "trusted issuer should eventually be inserted into issuer index via async loading",
        )
        .await;
    }
}
