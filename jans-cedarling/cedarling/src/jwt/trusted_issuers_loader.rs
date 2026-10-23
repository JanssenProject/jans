// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use chrono::Utc;
use std::{
    collections::{HashMap, hash_map::Entry},
    sync::{Arc, Mutex},
    time::Duration,
};
use thiserror::Error;
use tokio::sync::Notify;
use tokio_util::sync::CancellationToken;

use crate::http_utils::Backoff;
use crate::{
    JwtConfig, LogLevel,
    async_sleep::sleep,
    common::{
        issuer_utils::IssClaim,
        policy_store::{KeySource, TokenTypes},
    },
    http::HttpClient,
    jwt::{
        GetFromUrl, IssuerConfig, IssuerIndex, JwtLogEntry, JwtServiceInitError, KeyService,
        OpenIdConfig, TokenCache,
        loading_state::TrustedIssuerLoadingState,
        status_list::{InitForIssArgs, StatusListCache},
        validation::JwtValidatorCache,
    },
    jwt_config::{DEFAULT_JWKS_REFRESH_INTERVAL_SECS, TrustedIssuerLoaderConfig},
    log::{BaseLogEntry, LogEntry, LogWriter, Logger},
};

use crate::http::spawn_task;

#[derive(Error, Debug)]
pub(crate) enum TrustedIssuerLoaderError {
    #[error(
        "failed to acquire semaphore permit for concurrent issuer loading - this indicates a serious concurrency issue or resource exhaustion"
    )]
    SemaphoreAcquire,
    #[error(
        "discovery document at '{configuration}' declares issuer '{found}', \
         but the configured iss is '{expected}'"
    )]
    DiscoveryIssuerMismatch {
        configuration: String,
        expected: IssClaim,
        found: IssClaim,
    },
    #[error(
        "issuers '{}' resolve to iss '{iss}' with different JWKS ('{}'); none of them is loaded",
        .issuers.join("', '"),
        .jwks.join("', '")
    )]
    ConflictingJwks {
        iss: IssClaim,
        issuers: Vec<String>,
        jwks: Vec<String>,
    },
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
    pub(super) loading_state: Arc<TrustedIssuerLoadingState>,
    pub(super) http_client: HttpClient,
    pub(super) jwks_refresh_notifiers: Arc<Mutex<HashMap<IssClaim, Arc<Notify>>>>,
    pub(super) jwks_cancel_token: CancellationToken,
}

impl TrustedIssuerLoader {
    /// Load trusted issuers either synchronously or asynchronously based on configuration.
    pub(super) async fn load_trusted_issuers(
        &self,
        token_types: &TokenTypes,
    ) -> Result<(), JwtServiceInitError> {
        let trusted_issuers = unresolved_issuers(token_types);
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

/// Every JWT issuer of `token_types` with its token types, not yet resolved.
fn unresolved_issuers(token_types: &TokenTypes) -> Vec<IssuerConfig> {
    token_types
        .jwt_issuers()
        .iter()
        .map(|(name, issuer)| IssuerConfig {
            policy: issuer.clone(),
            tokens: token_types
                .jwt_tokens_of(name)
                .map(|(entity_type, metadata)| (entity_type.clone(), metadata.clone()))
                .collect(),
            openid_config: None,
        })
        .collect()
}

/// Loads every issuer in two phases. Key sources are resolved first, so that
/// issuers whose keys would collide under one `iss` are all rejected before any
/// of them is installed; the outcome then does not depend on task order.
async fn load_trusted_issuers(
    loader: &TrustedIssuerLoader,
    trusted_issuers: Vec<IssuerConfig>,
    workers: usize,
) -> Result<(), JwtServiceInitError> {
    let semaphore = Arc::new(tokio::sync::Semaphore::new(workers));
    let mut errors = Vec::new();

    // Load local JWKS once before processing issuers to avoid redundant work
    if loader.jwt_config.jwt_sig_validation
        && let Some(jwks) = loader.jwt_config.jwks.as_ref()
    {
        loader
            .key_service
            .insert_keys_from_str(jwks)
            .map_err(JwtServiceInitError::PrepareKeys)?;
    }

    let mut handles = Vec::new();
    for issuer in trusted_issuers {
        let permit = acquire(&semaphore).await?;
        let loader = loader.clone();
        handles.push(spawn_task(async move {
            let http_client = loader.http_client.clone();
            let issuer_id = issuer.policy.name.to_string();
            let resolved = resolve_trusted_issuer(&loader, issuer, &http_client).await;
            drop(permit);
            (issuer_id, resolved)
        }));
    }
    let mut resolved = Vec::with_capacity(handles.len());
    for handle in handles {
        match handle.await_result().await {
            (_, Ok(issuer)) => resolved.push(issuer),
            (issuer_id, Err(error)) => {
                record_failure(loader, &issuer_id, &error);
                errors.push(error);
            },
        }
    }

    let (resolved, rejected) = if loader.jwt_config.jwt_sig_validation {
        reject_conflicting_key_sources(resolved)
    } else {
        // No keys are loaded, so a shared `iss` cannot plant any.
        (resolved, Vec::new())
    };
    for (issuer_id, error) in rejected {
        loader.logger.log_any(JwtLogEntry::new(
            format!("trusted issuer '{issuer_id}' not loaded: {error}"),
            Some(LogLevel::ERROR),
        ));
        loader.loading_state.add_trusted_issuer_failed(issuer_id);
        errors.push(error.into());
    }

    let mut handles = Vec::new();
    for issuer in resolved {
        let permit = acquire(&semaphore).await?;
        let loader = loader.clone();
        handles.push(spawn_task(async move {
            let issuer_id = issuer.config.policy.name.to_string();
            let installed = install_trusted_issuer(&loader, issuer).await;
            drop(permit);
            (issuer_id, installed)
        }));
    }
    for handle in handles {
        if let (issuer_id, Err(error)) = handle.await_result().await {
            record_failure(loader, &issuer_id, &error);
            errors.push(error);
        }
    }

    loader.check_keys_loaded();

    if !errors.is_empty() {
        return Err(JwtServiceInitError::LoadTrustedIssuersAggregate(
            errors.into(),
        ));
    }

    Ok(())
}

async fn acquire(
    semaphore: &Arc<tokio::sync::Semaphore>,
) -> Result<tokio::sync::OwnedSemaphorePermit, TrustedIssuerLoaderError> {
    semaphore
        .clone()
        .acquire_owned()
        .await
        .map_err(|_| TrustedIssuerLoaderError::SemaphoreAcquire)
}

/// Logs an issuer that could not be loaded and marks it failed, without failing
/// the others.
fn record_failure(loader: &TrustedIssuerLoader, issuer_id: &str, error: &JwtServiceInitError) {
    loader.logger.log_any(
        LogEntry::new(BaseLogEntry::new_system_opt_request_id(
            LogLevel::WARN,
            None,
        ))
        .set_message(format!("Could not load trusted issuer: {issuer_id}"))
        .set_error(error.to_string()),
    );
    loader
        .loading_state
        .add_trusted_issuer_failed(issuer_id.to_string());
}

/// An issuer whose key source is known, ready to be installed.
struct ResolvedIssuer {
    config: IssuerConfig,
    /// What tokens of this issuer carry in `iss`.
    iss: IssClaim,
    /// Unknown when discovery was skipped because no validation needs it.
    jwks_uri: Option<url::Url>,
}

/// Splits off every issuer whose `iss` is shared with another issuer that loads
/// its keys from a different JWKS. Keys are stored per `iss`, so either key set
/// would also validate the other issuer's tokens; with no order between them,
/// none of them is trusted.
fn reject_conflicting_key_sources(
    resolved: Vec<ResolvedIssuer>,
) -> (Vec<ResolvedIssuer>, Vec<(String, TrustedIssuerLoaderError)>) {
    let mut sources_by_iss: HashMap<&IssClaim, Vec<(&str, &url::Url)>> = HashMap::new();
    for issuer in &resolved {
        if let Some(jwks_uri) = &issuer.jwks_uri {
            sources_by_iss
                .entry(&issuer.iss)
                .or_default()
                .push((issuer.config.policy.name.as_str(), jwks_uri));
        }
    }

    let mut conflicts: HashMap<IssClaim, (Vec<String>, Vec<String>)> = HashMap::new();
    for (iss, sources) in sources_by_iss {
        let mut jwks: Vec<String> = sources.iter().map(|(_, uri)| uri.to_string()).collect();
        jwks.sort();
        jwks.dedup();
        if jwks.len() > 1 {
            let mut issuers: Vec<String> =
                sources.iter().map(|(id, _)| (*id).to_string()).collect();
            issuers.sort();
            conflicts.insert(iss.clone(), (issuers, jwks));
        }
    }

    let (rejected, kept): (Vec<_>, Vec<_>) = resolved
        .into_iter()
        .partition(|issuer| issuer.jwks_uri.is_some() && conflicts.contains_key(&issuer.iss));
    let rejected = rejected
        .into_iter()
        .map(|issuer| {
            let (issuers, jwks) = conflicts[&issuer.iss].clone();
            let error = TrustedIssuerLoaderError::ConflictingJwks {
                iss: issuer.iss,
                issuers,
                jwks,
            };
            (issuer.config.policy.name.to_string(), error)
        })
        .collect();
    (kept, rejected)
}

/// Resolves and installs one issuer, without checking it against others.
#[cfg(test)]
pub(super) async fn load_trusted_issuer(
    loader: &TrustedIssuerLoader,
    issuer: IssuerConfig,
    http_client: HttpClient,
) -> Result<(), JwtServiceInitError> {
    let resolved = resolve_trusted_issuer(loader, issuer, &http_client).await?;
    install_trusted_issuer(loader, resolved).await
}

/// Finds where an issuer's keys come from: its discovery document, checked
/// against the expected `iss`, or its static JWKS.
async fn resolve_trusted_issuer(
    loader: &TrustedIssuerLoader,
    mut config: IssuerConfig,
    http_client: &HttpClient,
) -> Result<ResolvedIssuer, JwtServiceInitError> {
    // this is what we expect to find in the JWT `iss` claim
    let iss_claim = config.policy.iss_claim().clone();

    if let KeySource::Discovery { configuration } = &config.policy.key_source
        && (loader.jwt_config.jwt_sig_validation || loader.jwt_config.jwt_status_validation)
    {
        config.openid_config = Some(
            discover(
                configuration,
                &iss_claim,
                config.policy.name.as_str(),
                loader.logger.as_ref(),
                http_client,
            )
            .await?,
        );
    }

    let jwks_uri = match &config.policy.key_source {
        KeySource::Discovery { .. } => config
            .openid_config
            .as_ref()
            .map(|openid| openid.jwks_uri.clone()),
        KeySource::Static { jwks } => Some(jwks.clone()),
    };

    Ok(ResolvedIssuer {
        config,
        iss: iss_claim,
        jwks_uri,
    })
}

/// Fetches the keys of a resolved issuer and makes it available for validation.
async fn install_trusted_issuer(
    loader: &TrustedIssuerLoader,
    issuer: ResolvedIssuer,
) -> Result<(), JwtServiceInitError> {
    let ResolvedIssuer {
        config: iss_config,
        iss: iss_claim,
        jwks_uri,
    } = issuer;
    let http_client = loader.http_client.clone();

    if loader.jwt_config.jwt_sig_validation
        && let Some(jwks_uri) = &jwks_uri
    {
        loader
            .key_service
            .fetch_keys(
                &iss_claim,
                jwks_uri,
                loader.logger.as_ref(),
                http_client.clone(),
            )
            .await?;
    }

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
                InitForIssArgs {
                    validators: &loader.validators,
                    key_service: &loader.key_service,
                    token_cache: loader.token_cache.clone(),
                    logger: loader.logger.clone(),
                    http_client,
                    // `JwtConfig` has already been normalized in `JwtService::new`,
                    // so this value is guaranteed to be in the safe range.
                    refresh_interval_max: std::time::Duration::from_secs(
                        loader.jwt_config.status_list_refresh_interval_max,
                    ),
                    cancel_tkn: loader.jwks_cancel_token.clone(),
                },
            )
            .await?;
    }

    if loader.jwt_config.jwt_sig_validation
        && let Some(jwks_uri) = jwks_uri
    {
        spawn_jwks_refresh(loader, iss_claim.clone(), jwks_uri);
    }

    loader.issuer_configs.insert(iss_claim, iss_config);

    Ok(())
}

/// Starts the background task that keeps the JWKS of `iss` fresh, once per `iss`:
/// issuers sharing an `iss` share its keys, as conflicting JWKS were rejected.
fn spawn_jwks_refresh(loader: &TrustedIssuerLoader, iss: IssClaim, jwks_uri: url::Url) {
    let notify = Arc::new(Notify::new());

    let initial_interval = Duration::from_secs(
        loader
            .jwt_config
            .jwks_refresh_interval
            .unwrap_or(DEFAULT_JWKS_REFRESH_INTERVAL_SECS),
    );
    let min_interval = Duration::from_secs(loader.jwt_config.jwks_refresh_min_interval);
    let config_override = loader.jwt_config.jwks_refresh_interval;

    {
        let mut notifiers = loader
            .jwks_refresh_notifiers
            .lock()
            .expect("acquire jwks_refresh_notifiers lock");
        match notifiers.entry(iss.clone()) {
            Entry::Occupied(_) => return,
            Entry::Vacant(slot) => {
                slot.insert(notify.clone());
            },
        }
    }

    spawn_task(keep_jwks_updated(JwksRefreshParams {
        key_service: loader.key_service.clone(),
        iss,
        jwks_uri,
        initial_interval,
        min_interval,
        config_override,
        notify,
        logger: loader.logger.clone(),
        cancel_tkn: loader.jwks_cancel_token.clone(),
        http_client: loader.http_client.clone(),
    }));
}

/// Fetches the `OpenID` configuration of a discovery issuer and checks that it
/// is for the expected `iss` (OIDC Discovery §4.3).
async fn discover(
    configuration: &url::Url,
    expected: &IssClaim,
    issuer_id: &str,
    logger: Option<&Logger>,
    http_client: &HttpClient,
) -> Result<OpenIdConfig, JwtServiceInitError> {
    let openid_config = OpenIdConfig::get_from_url(configuration, http_client)
        .await
        .inspect_err(|e| {
            logger.log_any(JwtLogEntry::new(
                format!(
                    "failed to get openid configuration for trusted issuer: '{issuer_id}': {e}"
                ),
                Some(LogLevel::ERROR),
            ));
        })?;
    if openid_config.issuer != *expected {
        return Err(TrustedIssuerLoaderError::DiscoveryIssuerMismatch {
            configuration: configuration.to_string(),
            expected: expected.clone(),
            found: openid_config.issuer,
        }
        .into());
    }
    Ok(openid_config)
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

struct JwksRefreshParams {
    key_service: Arc<KeyService>,
    iss: IssClaim,
    jwks_uri: url::Url,
    initial_interval: Duration,
    min_interval: Duration,
    config_override: Option<u64>,
    notify: Arc<Notify>,
    logger: Option<Logger>,
    cancel_tkn: CancellationToken,
    http_client: HttpClient,
}

/// Background task that periodically re-fetches JWKS for a single trusted issuer
async fn keep_jwks_updated(params: JwksRefreshParams) {
    let JwksRefreshParams {
        key_service,
        iss,
        jwks_uri,
        initial_interval,
        min_interval,
        config_override,
        notify,
        logger,
        cancel_tkn,
        http_client,
    } = params;

    let mut interval = initial_interval;
    let mut last_refresh = Utc::now() - interval;
    let mut backoff = Backoff::default_fixed();

    loop {
        tokio::select! {
            () = sleep(interval) => {},
            () = notify.notified() => {
                let elapsed = Utc::now().signed_duration_since(last_refresh).num_seconds().cast_unsigned();
                if elapsed < min_interval.as_secs() {
                    sleep(Duration::from_secs(min_interval.as_secs() - elapsed)).await;
                }
            },
            () = cancel_tkn.cancelled() => { break; },
        }

        logger.log_any(JwtLogEntry::new(
            format!("refreshing JWKS for issuer '{}'", iss.as_str()),
            Some(LogLevel::DEBUG),
        ));

        match key_service
            .refresh_keys(&iss, &jwks_uri, logger.as_ref(), &http_client)
            .await
        {
            Ok(max_age) => {
                last_refresh = Utc::now();
                backoff.reset();

                if let Some(config_secs) = config_override {
                    interval = Duration::from_secs(config_secs);
                } else if let Some(max_age_secs) = max_age {
                    interval = if max_age_secs == 0 {
                        min_interval
                    } else {
                        Duration::from_secs(max_age_secs)
                    };
                }

                logger.log_any(JwtLogEntry::new(
                    format!(
                        "refreshed JWKS for issuer '{}', next refresh in {}s",
                        iss.as_str(),
                        interval.as_secs(),
                    ),
                    Some(LogLevel::INFO),
                ));
            },
            Err(err) => {
                logger.log_any(JwtLogEntry::new(
                    format!(
                        "failed to refresh JWKS for issuer '{}': {}",
                        iss.as_str(),
                        err,
                    ),
                    Some(LogLevel::ERROR),
                ));

                tokio::select! {
                    _ = backoff.snooze() => {},
                    () = cancel_tkn.cancelled() => { break; },
                }
            },
        }
    }
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::common::policy_store::{IssuerName, TokenEntityMetadata, TrustedIssuer};
    use crate::http::HttpClientConfig;
    use crate::jwt::key_service::DecodingKeyInfo;
    use crate::jwt::test_utils::{
        MockServer, create_failing_trusted_issuer, create_unreachable_trusted_issuer,
        generate_jwks, generate_keypair_hs256,
    };
    use crate::{common::issuer_utils::IssClaim, jwt_config::WorkersCount};
    use jsonwebtoken::Algorithm;
    use mockito::Server;
    use serde_json::json;
    use std::collections::{HashMap, HashSet};
    use std::sync::{Arc, LazyLock};
    use tokio::time::{Duration, sleep};
    use url::Url;

    static HTTP_CLIENT: LazyLock<HttpClient> = LazyLock::new(|| {
        HttpClient::new(HttpClientConfig {
            max_retries: 0,
            retry_delay: Duration::from_millis(3),
            request_timeout: Duration::from_millis(500),
            max_response_size_bytes: None,
        })
        .expect("http client should be constructed")
    });

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

    /// Token types with each issuer named by its key and declaring one token
    /// type in its own namespace, so issuers loaded together never conflict.
    fn token_types(issuers: HashMap<String, TrustedIssuer>) -> TokenTypes {
        TokenTypes::build(
            issuers.into_iter().map(|(id, mut issuer)| {
                let tokens = HashMap::from([(
                    format!("{id}::Access_token"),
                    TokenEntityMetadata::builder().build(),
                )]);
                issuer.name = IssuerName::for_test(&id);
                issuer.with_tokens(tokens)
            }),
            [],
        )
        .expect("test token types should build")
    }

    /// One issuer ready to load, named `id`.
    fn unresolved(id: &str, issuer: TrustedIssuer) -> IssuerConfig {
        let token_types = token_types(HashMap::from([(id.to_string(), issuer)]));
        let mut issuers = unresolved_issuers(&token_types);
        issuers.pop().expect("one issuer should be loaded")
    }

    /// Helper to create a `TrustedIssuerLoader` with proper loading state initialized
    /// with the given number of issuers.
    fn create_loader(jwt_config: JwtConfig, issuer_count: usize) -> TrustedIssuerLoader {
        TrustedIssuerLoader {
            jwt_config,
            status_lists: StatusListCache::default(),
            issuer_configs: Arc::new(IssuerIndex::new()),
            validators: Arc::new(JwtValidatorCache::default()),
            key_service: Arc::new(KeyService::new()),
            token_cache: TokenCache::default(),
            logger: None,
            loading_state: Arc::new(TrustedIssuerLoadingState::new(issuer_count)),
            http_client: HTTP_CLIENT.clone(),
            jwks_refresh_notifiers: Arc::new(Mutex::new(HashMap::new())),
            jwks_cancel_token: CancellationToken::new(),
        }
    }

    /// Tests synchronous loading of a single trusted issuer.
    ///
    /// Verifies that a trusted issuer can be loaded successfully in sync mode
    /// and that the issuer configuration is properly inserted into the index.
    #[tokio::test]
    async fn load_single_trusted_issuer_sync() {
        let server = MockServer::new_with_defaults()
            .await
            .expect("mock server should start");
        let trusted_issuer = server.trusted_issuer();

        let jwt_config = JwtConfig {
            jwt_sig_validation: false,
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::default(),
            ..Default::default()
        };

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("test_issuer".to_string(), trusted_issuer);

        let loader = create_loader(jwt_config, trusted_issuers.len());

        let result = loader
            .load_trusted_issuers(&token_types(trusted_issuers))
            .await;
        result.expect("should load single trusted issuer successfully in sync mode");

        // Verify issuer config was inserted
        let issuer_claim = server.issuer();
        assert!(
            loader.issuer_configs.contains_iss(&issuer_claim),
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
        let server1 = MockServer::new_with_defaults()
            .await
            .expect("mock server should start");
        let server2 = MockServer::new_with_defaults()
            .await
            .expect("mock server should start");

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

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("issuer1".to_string(), trusted_issuer_1);
        trusted_issuers.insert("issuer2".to_string(), trusted_issuer_2);

        let loader = create_loader(jwt_config, trusted_issuers.len());

        let result = loader
            .load_trusted_issuers(&token_types(trusted_issuers))
            .await;
        result.expect("should load multiple trusted issuers successfully in async mode");

        // Verify both issuers were loaded with retry for async operations
        let issuer_claim1 = server1.issuer();
        let issuer_claim2 = server2.issuer();
        retry_assert(
            || async { loader.issuer_configs.contains_iss(&issuer_claim1) },
            20,
            10,
            "first trusted issuer should be inserted into issuer index",
        )
        .await;
        retry_assert(
            || async { loader.issuer_configs.contains_iss(&issuer_claim2) },
            20,
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
        trusted_issuer.set_configuration_endpoint(
            Url::parse(&(server.url() + "/.well-known/openid-configuration")).unwrap(),
        );

        let jwt_config = JwtConfig {
            jwt_sig_validation: true, // Requires OpenID config
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::default(),
            ..Default::default()
        };

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("failing_issuer".to_string(), trusted_issuer);

        let loader = create_loader(jwt_config, trusted_issuers.len());

        let result = loader
            .load_trusted_issuers(&token_types(trusted_issuers))
            .await;
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
        trusted_issuer.set_configuration_endpoint(
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

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("failing_issuer".to_string(), trusted_issuer);

        let loader = create_loader(jwt_config, trusted_issuers.len());

        let result = loader
            .load_trusted_issuers(&token_types(trusted_issuers))
            .await;
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

        let loader = create_loader(jwt_config, 0);

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
            loading_state: Arc::new(TrustedIssuerLoadingState::new(0)),
            http_client: HTTP_CLIENT.clone(),
            jwks_refresh_notifiers: Arc::new(Mutex::new(HashMap::new())),
            jwks_cancel_token: CancellationToken::new(),
        };

        loader_with_keys.check_keys_loaded();
    }

    /// Tests loading trusted issuers with JWKS provided in configuration.
    ///
    /// Verifies that JWKS from the configuration are properly inserted into
    /// the key service when signature validation is enabled.
    #[tokio::test]
    async fn load_with_jwks_config() {
        let server = MockServer::new_with_defaults()
            .await
            .expect("mock server should start");
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

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("test_issuer".to_string(), trusted_issuer);

        let loader = create_loader(jwt_config, trusted_issuers.len());

        let result = loader
            .load_trusted_issuers(&token_types(trusted_issuers))
            .await;
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
        let server = MockServer::new_with_defaults()
            .await
            .expect("mock server should start");
        let trusted_issuer = server.trusted_issuer();

        let jwt_config = JwtConfig {
            jwt_sig_validation: true,
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::default(),
            ..Default::default()
        };

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("test_issuer".to_string(), trusted_issuer);

        let loader = create_loader(jwt_config, trusted_issuers.len());

        let result = loader
            .load_trusted_issuers(&token_types(trusted_issuers))
            .await;
        result.expect("should load trusted issuer successfully with signature validation enabled");

        // Verify issuer config was inserted
        let issuer_claim = server.issuer();
        assert!(
            loader.issuer_configs.contains_iss(&issuer_claim),
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

    fn sig_validation_config() -> JwtConfig {
        JwtConfig {
            jwt_sig_validation: true,
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::default(),
            ..Default::default()
        }
    }

    /// Serves `keys` as a JWKS at `path`, expecting `hits` requests.
    fn mock_jwks(server: &mut mockito::ServerGuard, path: &str, hits: usize) -> mockito::Mock {
        let keys = generate_keypair_hs256(Some("static_key")).expect("should generate keys");
        server
            .mock("GET", path)
            .with_status(200)
            .with_header("content-type", "application/json")
            .with_body(json!({ "keys": generate_jwks(&[keys]).keys }).to_string())
            .expect(hits)
            .create()
    }

    /// Serves a discovery document at `path` declaring `issuer`.
    fn mock_discovery(
        server: &mut mockito::ServerGuard,
        path: &str,
        issuer: &str,
        jwks_path: &str,
        hits: usize,
    ) -> mockito::Mock {
        server
            .mock("GET", path)
            .with_status(200)
            .with_header("content-type", "application/json")
            .with_body(
                json!({ "issuer": issuer, "jwks_uri": server.url() + jwks_path }).to_string(),
            )
            .expect(hits)
            .create()
    }

    #[tokio::test]
    async fn discovery_issuer_mismatch_fails_to_load() {
        let mut server = Server::new_async().await;
        let discovery = mock_discovery(
            &mut server,
            "/.well-known/openid-configuration",
            "https://other.example",
            "/jwks",
            1,
        );
        let jwks = mock_jwks(&mut server, "/jwks", 0);
        let mut issuer = TrustedIssuer::default();
        issuer.set_configuration_endpoint(
            Url::parse(&(server.url() + "/.well-known/openid-configuration")).expect("valid url"),
        );
        let loader = create_loader(sig_validation_config(), 1);

        let err = load_trusted_issuer(&loader, unresolved("jans", issuer), HTTP_CLIENT.clone())
            .await
            .expect_err("a discovery document for another issuer must not load");
        assert!(
            matches!(
                &err,
                JwtServiceInitError::LoadTrustedIssuers(
                    TrustedIssuerLoaderError::DiscoveryIssuerMismatch { expected, found, .. }
                ) if *expected == IssClaim::new(&server.url())
                    && *found == IssClaim::new("https://other.example")
            ),
            "expected DiscoveryIssuerMismatch naming both issuers, got: {err:?}"
        );
        assert!(
            !loader
                .issuer_configs
                .contains_iss(&IssClaim::new(&server.url())),
            "a mismatched issuer must not be indexed"
        );
        assert!(
            !loader.key_service.has_keys(),
            "no keys should be fetched for a mismatched issuer"
        );
        discovery.assert();
        jwks.assert();
    }

    #[tokio::test]
    async fn path_based_discovery_issuer_loads() {
        let mut server = Server::new_async().await;
        let iss = server.url() + "/realms/a";
        let discovery = mock_discovery(
            &mut server,
            "/realms/a/.well-known/openid-configuration",
            &iss,
            "/jwks",
            1,
        );
        let jwks = mock_jwks(&mut server, "/jwks", 1);
        let mut issuer = TrustedIssuer::default();
        issuer.set_configuration_endpoint(
            Url::parse(&(iss.clone() + "/.well-known/openid-configuration")).expect("valid url"),
        );
        let loader = create_loader(sig_validation_config(), 1);

        load_trusted_issuer(&loader, unresolved("jans", issuer), HTTP_CLIENT.clone())
            .await
            .expect("a path-based issuer whose discovery matches should load");
        assert!(
            loader.issuer_configs.contains_iss(&IssClaim::new(&iss)),
            "the issuer should be indexed by its full iss, path included"
        );
        discovery.assert();
        jwks.assert();
    }

    #[tokio::test]
    async fn static_issuer_loads_keys_without_discovery() {
        let mut server = Server::new_async().await;
        let url = server.url();
        let discovery = mock_discovery(
            &mut server,
            "/.well-known/openid-configuration",
            &url,
            "/keys",
            0,
        );
        let jwks = mock_jwks(&mut server, "/keys", 1);
        let iss = Url::parse(&server.url()).expect("valid url");
        let issuer = TrustedIssuer::new(
            IssuerName::for_test("Partner"),
            iss.clone(),
            KeySource::Static {
                jwks: Url::parse(&(server.url() + "/keys")).expect("valid url"),
            },
        );
        let loader = create_loader(sig_validation_config(), 1);

        load_trusted_issuer(&loader, unresolved("partner", issuer), HTTP_CLIENT.clone())
            .await
            .expect("a static issuer should load");

        let key_info = DecodingKeyInfo {
            issuer: Some(IssClaim::new(iss.as_str())),
            kid: Some("static_key".to_string()),
            algorithm: Algorithm::HS256,
        };
        assert!(
            loader.key_service.get_key(&key_info).is_some(),
            "keys from the static JWKS should be stored under the configured iss"
        );
        discovery.assert();
        jwks.assert();
    }

    /// Loads `issuers` the way `JwtService` does, with two workers so the
    /// issuers race each other.
    async fn load_concurrently(
        issuers: Vec<(&str, TrustedIssuer)>,
    ) -> (TrustedIssuerLoader, Result<(), JwtServiceInitError>) {
        let config = JwtConfig {
            trusted_issuer_loader: TrustedIssuerLoaderConfig::Sync {
                workers: WorkersCount::new(2),
            },
            ..sig_validation_config()
        };
        let loader = create_loader(config, issuers.len());
        let issuers = issuers
            .into_iter()
            .map(|(id, issuer)| (id.to_string(), issuer))
            .collect();
        let result = loader.load_trusted_issuers(&token_types(issuers)).await;
        (loader, result)
    }

    /// The per-issuer errors of a failed load.
    fn issuer_errors(err: JwtServiceInitError) -> Vec<JwtServiceInitError> {
        match err {
            JwtServiceInitError::LoadTrustedIssuersAggregate(errors) => errors.0,
            other => vec![other],
        }
    }

    fn static_issuer(name: &str, iss: &str, jwks: &str) -> TrustedIssuer {
        TrustedIssuer::new(
            IssuerName::for_test(name),
            Url::parse(iss).expect("valid url"),
            KeySource::Static {
                jwks: Url::parse(jwks).expect("valid url"),
            },
        )
    }

    fn discovery_issuer(name: &str, iss: &str) -> TrustedIssuer {
        let mut issuer = TrustedIssuer::default();
        issuer.name = IssuerName::for_test(name);
        issuer.set_configuration_endpoint(
            Url::parse(&format!("{iss}/.well-known/openid-configuration")).expect("valid url"),
        );
        issuer
    }

    /// Two key sources only found to share an `iss` after discovery: there is no
    /// order between them, so neither is trusted, and neither key set is fetched.
    #[tokio::test]
    async fn key_sources_sharing_an_iss_with_different_jwks_are_both_rejected() {
        let mut server = Server::new_async().await;
        let iss = server.url();
        let discovery = mock_discovery(
            &mut server,
            "/.well-known/openid-configuration",
            &iss,
            "/discovered-keys",
            1,
        );
        let static_jwks = mock_jwks(&mut server, "/static-keys", 0);
        let discovered_jwks = mock_jwks(&mut server, "/discovered-keys", 0);

        let (loader, result) = load_concurrently(vec![
            (
                "Partner",
                static_issuer("Partner", &iss, &format!("{iss}/static-keys")),
            ),
            ("Other", discovery_issuer("Other", &iss)),
        ])
        .await;

        let errors = issuer_errors(result.expect_err("both issuers should fail to load"));
        assert_eq!(
            errors.len(),
            2,
            "both issuers should be reported: {errors:?}"
        );
        for error in &errors {
            assert!(
                matches!(
                    error,
                    JwtServiceInitError::LoadTrustedIssuers(TrustedIssuerLoaderError::ConflictingJwks {
                        iss: conflict_iss,
                        issuers,
                        jwks,
                    }) if *conflict_iss == IssClaim::new(&iss)
                        && issuers == &["Other".to_string(), "Partner".to_string()]
                        && jwks.len() == 2
                ),
                "expected ConflictingJwks naming both issuers and both JWKS, got: {error:?}"
            );
        }
        assert!(
            !loader.issuer_configs.contains_iss(&IssClaim::new(&iss)),
            "neither issuer should be loaded"
        );
        assert_eq!(
            loader.loading_state.failed_issuers(),
            HashSet::from(["Other".to_string(), "Partner".to_string()]),
            "both issuers should be marked failed"
        );
        discovery.assert();
        static_jwks.assert();
        discovered_jwks.assert();
    }

    /// Issuers may share an `iss` when they agree on its keys: a token carrying
    /// that `iss` then verifies the same way whichever token type names it.
    #[tokio::test]
    async fn key_sources_sharing_an_iss_and_jwks_both_load() {
        let mut server = Server::new_async().await;
        let iss = server.url();
        let _jwks = mock_jwks(&mut server, "/keys", 2);

        let (loader, result) = load_concurrently(vec![
            ("Acme", static_issuer("Acme", &iss, &format!("{iss}/keys"))),
            ("Beta", static_issuer("Beta", &iss, &format!("{iss}/keys"))),
        ])
        .await;

        result.expect("issuers sharing an iss and its keys should both load");
        assert_eq!(
            loader.issuer_configs.loaded_issuer_ids(),
            HashSet::from(["Acme".to_string(), "Beta".to_string()]),
            "both issuers should be loaded"
        );
    }

    /// Issuers sharing an `iss` get one JWKS refresh task, so the notifier that
    /// wakes it is never replaced by one no task listens on.
    #[tokio::test]
    async fn one_jwks_refresh_per_iss() {
        let loader = create_loader(sig_validation_config(), 2);
        let iss = IssClaim::new("https://idp.example");
        let jwks = Url::parse("https://idp.example/keys").expect("valid url");
        let notifier = || {
            loader
                .jwks_refresh_notifiers
                .lock()
                .expect("acquire jwks_refresh_notifiers lock")
                .get(&iss)
                .cloned()
                .expect("a refresh should be registered for the iss")
        };

        spawn_jwks_refresh(&loader, iss.clone(), jwks.clone());
        let first = notifier();
        spawn_jwks_refresh(&loader, iss.clone(), jwks);
        loader.jwks_cancel_token.cancel();

        assert!(
            Arc::ptr_eq(&first, &notifier()),
            "a second issuer for the iss should reuse the running refresh"
        );
    }

    /// An issuer that fails to resolve its key source takes no part in the
    /// conflict check, so it cannot block another issuer with the same `iss`.
    #[tokio::test]
    async fn failed_discovery_does_not_block_another_issuer_for_the_iss() {
        let mut server = Server::new_async().await;
        let iss = server.url();
        let _discovery = server
            .mock("GET", "/.well-known/openid-configuration")
            .with_status(500)
            .create();
        let _jwks = mock_jwks(&mut server, "/static-keys", 1);

        let (loader, result) = load_concurrently(vec![
            ("Broken", discovery_issuer("Broken", &iss)),
            (
                "Partner",
                static_issuer("Partner", &iss, &format!("{iss}/static-keys")),
            ),
        ])
        .await;

        let errors = issuer_errors(result.expect_err("the broken issuer should be reported"));
        assert_eq!(
            errors.len(),
            1,
            "only the broken issuer should fail: {errors:?}"
        );
        assert_eq!(
            loader.issuer_configs.loaded_issuer_ids(),
            HashSet::from(["Partner".to_string()]),
            "the issuer whose keys resolved should still load"
        );
        assert_eq!(
            loader.loading_state.failed_issuers(),
            HashSet::from(["Broken".to_string()]),
            "only the broken issuer should be marked failed"
        );
    }

    #[tokio::test]
    async fn issuer_with_several_tokens_is_fetched_once() {
        let server = MockServer::new_with_defaults()
            .await
            .expect("mock server should start");
        let issuer = server.trusted_issuer().with_tokens(
            [
                "Jans::Access_token",
                "Jans::Id_token",
                "Jans::Userinfo_token",
            ]
            .into_iter()
            .map(|entity_type| (entity_type.into(), TokenEntityMetadata::builder().build()))
            .collect(),
        );
        let token_types = TokenTypes::build([issuer], []).expect("test token types should build");
        let loader = create_loader(sig_validation_config(), 1);

        loader
            .load_trusted_issuers(&token_types)
            .await
            .expect("issuer should load");

        // Both mocks expect exactly one request.
        server
            .endpoints
            .oidc
            .as_ref()
            .expect("discovery mock")
            .assert();
        server.endpoints.jwks.as_ref().expect("jwks mock").assert();
    }

    /// Tests synchronous loading of a single trusted issuer with status validation enabled.
    ///
    /// Verifies that when `jwt_status_validation` is true, the status list cache is initialized
    /// for the issuer and contains the expected status list.
    #[tokio::test]
    async fn load_trusted_issuer_with_status_validation_sync() {
        let mut server = MockServer::new_with_defaults()
            .await
            .expect("mock server should start");
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

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("test_issuer".to_string(), trusted_issuer);

        let loader = create_loader(jwt_config, trusted_issuers.len());

        let result = loader
            .load_trusted_issuers(&token_types(trusted_issuers))
            .await;
        result.expect("should load trusted issuer successfully with status validation enabled");

        // Verify issuer config was inserted
        let issuer_claim = server.issuer();
        assert!(
            loader.issuer_configs.contains_iss(&issuer_claim),
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
        let server1 = MockServer::new_with_defaults()
            .await
            .expect("mock server should start");
        let server2 = MockServer::new_with_defaults()
            .await
            .expect("mock server should start");

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

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("issuer1".to_string(), trusted_issuer_1);
        trusted_issuers.insert("issuer2".to_string(), trusted_issuer_2);

        let loader = create_loader(jwt_config, trusted_issuers.len());

        let result = loader
            .load_trusted_issuers(&token_types(trusted_issuers))
            .await;
        result.expect("should load multiple trusted issuers successfully in async mode with signature validation");

        // Verify both issuers were loaded with retry for async operations
        let issuer_claim1 = server1.issuer();
        let issuer_claim2 = server2.issuer();
        retry_assert(
            || async { loader.issuer_configs.contains_iss(&issuer_claim1) },
            20,
            10,
            "first trusted issuer should be inserted into issuer index",
        )
        .await;
        retry_assert(
            || async { loader.issuer_configs.contains_iss(&issuer_claim2) },
            20,
            10,
            "second trusted issuer should be inserted into issuer index",
        )
        .await;

        // Verify keys are stored for both issuers with retry for async operations
        retry_assert(
            || async { loader.key_service.has_keys() },
            20,
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
            20,
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
            20,
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
            servers.push(
                MockServer::new_with_defaults()
                    .await
                    .expect("mock server should start"),
            );
        }

        let jwt_config = JwtConfig {
            jwt_sig_validation: true,
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::Async {
                workers: WorkersCount::new(NUM_ISSUERS),
            },
            ..Default::default()
        };

        let mut trusted_issuers = HashMap::new();
        for (i, server) in servers.iter().enumerate() {
            trusted_issuers.insert(format!("issuer{i}"), server.trusted_issuer());
        }

        let loader = create_loader(jwt_config, trusted_issuers.len());

        let result = loader
            .load_trusted_issuers(&token_types(trusted_issuers))
            .await;
        result.expect("should load multiple trusted issuers successfully with max workers");

        // Verify all issuers were loaded with retry for async operations
        for (i, server) in servers.iter().enumerate() {
            let issuer_claim = server.issuer();
            retry_assert(
                || async { loader.issuer_configs.contains_iss(&issuer_claim) },
                20,
                10,
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
            servers.push(
                MockServer::new_with_defaults()
                    .await
                    .expect("mock server should start"),
            );
        }

        let jwt_config = JwtConfig {
            jwt_sig_validation: false,
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::Sync {
                workers: WorkersCount::new(1),
            },
            ..Default::default()
        };

        let mut trusted_issuers = HashMap::new();
        for (i, server) in servers.iter().enumerate() {
            trusted_issuers.insert(format!("issuer{i}"), server.trusted_issuer());
        }

        let loader = create_loader(jwt_config, trusted_issuers.len());

        let result = loader
            .load_trusted_issuers(&token_types(trusted_issuers))
            .await;
        result.expect("should load many trusted issuers successfully with single worker");

        // Verify all issuers were loaded
        for (i, server) in servers.iter().enumerate() {
            let issuer_claim = server.issuer();
            assert!(
                loader.issuer_configs.contains_iss(&issuer_claim),
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
        let server = MockServer::new_with_defaults()
            .await
            .expect("mock server should start");
        let trusted_issuer = server.trusted_issuer();

        let jwt_config = JwtConfig {
            jwt_sig_validation: false,
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::Async {
                workers: WorkersCount::new(1),
            },
            ..Default::default()
        };

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("test_issuer".to_string(), trusted_issuer);

        let loader = create_loader(jwt_config, trusted_issuers.len());

        // Start timing
        let start = tokio::time::Instant::now();

        // in `impl GetFromUrl<OpenIdConfig> for OpenIdConfig{`
        // we have 5ms delay (for tests), so if initialization is sync it should take at least 5ms.

        let result = loader
            .load_trusted_issuers(&token_types(trusted_issuers))
            .await;
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
            || async { loader.issuer_configs.contains_iss(&issuer_claim) },
            20,
            10,
            "trusted issuer should eventually be inserted into issuer index via async loading",
        )
        .await;
    }

    /// Tests synchronous loading when one trusted issuer has an invalid URL.
    ///
    /// Verifies that sync mode returns an error when at least one issuer cannot be loaded,
    /// but other issuers are still loaded successfully.
    #[tokio::test]
    async fn load_trusted_issuer_with_invalid_url_sync() {
        // Create a valid mock server for successful issuer
        let valid_server = MockServer::new_with_defaults()
            .await
            .expect("mock server should start");
        let valid_issuer = valid_server.trusted_issuer();

        // Create an invalid issuer with a URL that will fail to fetch
        let mut invalid_issuer = TrustedIssuer::default();
        invalid_issuer.set_configuration_endpoint(
            Url::parse("invalid://example.com/.well-known/openid-configuration").unwrap(),
        );

        let jwt_config = JwtConfig {
            jwt_sig_validation: true, // Required to trigger OpenID config fetch
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::default(),
            ..Default::default()
        };

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("valid_issuer".to_string(), valid_issuer);
        trusted_issuers.insert("invalid_issuer".to_string(), invalid_issuer);

        let loader = create_loader(jwt_config, trusted_issuers.len());

        let result = loader
            .load_trusted_issuers(&token_types(trusted_issuers))
            .await;
        // Should return error because at least one issuer failed
        result.expect_err("sync mode should return error when an issuer has invalid URL");

        // Verify valid issuer was loaded despite the error
        let valid_issuer_claim = valid_server.issuer();
        assert!(
            loader.issuer_configs.contains_iss(&valid_issuer_claim),
            "valid issuer should be loaded even when another issuer fails in sync mode"
        );
    }

    /// Tests asynchronous loading when one trusted issuer has an invalid URL.
    ///
    /// Verifies that async mode returns Ok immediately and other issuers are loaded
    /// successfully while the failing issuer does not block the process.
    #[tokio::test]
    async fn load_trusted_issuer_with_invalid_url_async() {
        // Create two valid mock servers for successful issuers
        let valid_server1 = MockServer::new_with_defaults()
            .await
            .expect("mock server should start");
        let valid_server2 = MockServer::new_with_defaults()
            .await
            .expect("mock server should start");
        let valid_issuer1 = valid_server1.trusted_issuer();
        let valid_issuer2 = valid_server2.trusted_issuer();

        // Create an invalid issuer with a URL that will fail to fetch
        let mut invalid_issuer = TrustedIssuer::default();
        invalid_issuer.set_configuration_endpoint(
            Url::parse("invalid://example.com/.well-known/openid-configuration").unwrap(),
        );

        let jwt_config = JwtConfig {
            jwt_sig_validation: true,
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::Async {
                workers: WorkersCount::new(2),
            },
            ..Default::default()
        };

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("valid_issuer1".to_string(), valid_issuer1);
        trusted_issuers.insert("valid_issuer2".to_string(), valid_issuer2);
        trusted_issuers.insert("invalid_issuer".to_string(), invalid_issuer);

        let loader = create_loader(jwt_config, trusted_issuers.len());

        let result = loader
            .load_trusted_issuers(&token_types(trusted_issuers))
            .await;
        // Async mode should return Ok immediately even if some issuers fail
        result.expect(
            "async mode should not fail entire load process when an issuer has invalid URL",
        );

        // Verify both valid issuers are eventually loaded
        let valid_issuer_claim1 = valid_server1.issuer();
        let valid_issuer_claim2 = valid_server2.issuer();
        retry_assert(
            || async { loader.issuer_configs.contains_iss(&valid_issuer_claim1) },
            20,
            10,
            "first valid issuer should be inserted into issuer index",
        )
        .await;
        retry_assert(
            || async { loader.issuer_configs.contains_iss(&valid_issuer_claim2) },
            20,
            10,
            "second valid issuer should be inserted into issuer index",
        )
        .await;
    }

    /// Tests that `failed_trusted_issuer_ids()` correctly returns IDs of issuers that failed to load.
    ///
    /// Verifies that both sync and async modes properly track failed issuers and that the
    /// `TrustedIssuerLoadingInfo` trait provides accurate information about loading status.
    #[tokio::test]
    async fn failed_trusted_issuer_ids_tracks_failures() {
        // Create a mix of successful and failing issuers
        let valid_server = MockServer::new_with_defaults()
            .await
            .expect("mock server should start");
        let valid_issuer = valid_server.trusted_issuer();

        let failing_server = MockServer::new_with_failing_oidc().await.unwrap();
        let failing_issuer = failing_server.trusted_issuer();

        let unreachable_issuer = create_unreachable_trusted_issuer("unreachable_issuer");

        let jwt_config = JwtConfig {
            jwt_sig_validation: true, // Required to trigger OpenID config fetch
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::default(),
            ..Default::default()
        };

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("valid_issuer".to_string(), valid_issuer);
        trusted_issuers.insert("failing_issuer".to_string(), failing_issuer);
        trusted_issuers.insert("unreachable_issuer".to_string(), unreachable_issuer);

        let loader = create_loader(jwt_config, trusted_issuers.len());

        // In sync mode, load_trusted_issuers returns error, but some issuers may still be loaded
        let result = loader
            .load_trusted_issuers(&token_types(trusted_issuers))
            .await;
        // Should return error because at least one issuer failed
        result.expect_err("sync mode should return error when issuers fail");

        // Verify valid issuer was loaded
        let valid_issuer_claim = valid_server.issuer();
        assert!(
            loader.issuer_configs.contains_iss(&valid_issuer_claim),
            "valid issuer should be loaded even when other issuers fail"
        );

        // Verify failed issuer IDs are tracked
        let failed_ids = loader.loading_state.failed_issuers();
        assert_eq!(
            failed_ids.len(),
            2,
            "should have 2 failed issuers (failing_issuer and unreachable_issuer)"
        );
        assert!(
            failed_ids.contains("failing_issuer"),
            "failed_issuers should contain 'failing_issuer'"
        );
        assert!(
            failed_ids.contains("unreachable_issuer"),
            "failed_issuers should contain 'unreachable_issuer'"
        );
        assert!(
            !failed_ids.contains("valid_issuer"),
            "failed_issuers should not contain 'valid_issuer'"
        );

        // Verify loading state counts
        assert_eq!(
            loader.loading_state.total_issuers(),
            3,
            "loading state should track total issuers"
        );

        assert_eq!(
            loader.loading_state.failed_issuers().len(),
            2,
            "loading state should have 1 failed issuer"
        );
    }

    /// Tests that `failed_trusted_issuer_ids()` works correctly in async loading mode.
    ///
    /// Verifies that async mode tracks failed issuers even when the overall load process
    /// returns success immediately.
    #[tokio::test]
    async fn failed_trusted_issuer_ids_async_mode() {
        // Create a mix of successful and failing issuers
        let valid_server = MockServer::new_with_defaults()
            .await
            .expect("mock server should start");
        let valid_issuer = valid_server.trusted_issuer();

        let failing_issuer = create_failing_trusted_issuer("failing_issuer");

        let jwt_config = JwtConfig {
            jwt_sig_validation: true,
            jwt_status_validation: false,
            trusted_issuer_loader: TrustedIssuerLoaderConfig::Async {
                workers: WorkersCount::new(2),
            },
            ..Default::default()
        };

        let mut trusted_issuers = HashMap::new();
        trusted_issuers.insert("valid_issuer".to_string(), valid_issuer);
        trusted_issuers.insert("failing_issuer".to_string(), failing_issuer);

        let loader = create_loader(jwt_config, trusted_issuers.len());

        // Async mode should return Ok immediately
        let result = loader
            .load_trusted_issuers(&token_types(trusted_issuers))
            .await;
        result.expect("async mode should not fail entire load process");

        // Wait for async loading to complete
        let valid_issuer_claim = valid_server.issuer();
        retry_assert(
            || async { loader.issuer_configs.contains_iss(&valid_issuer_claim) },
            20,
            10,
            "valid issuer should eventually be loaded via async loading",
        )
        .await;

        // Verify failed issuer IDs are tracked
        let failed_ids = loader.loading_state.failed_issuers();
        assert_eq!(
            failed_ids.len(),
            1,
            "should have 1 failed issuer (failing_issuer)"
        );
        assert!(
            failed_ids.contains("failing_issuer"),
            "failed_issuers should contain 'failing_issuer'"
        );
        assert!(
            !failed_ids.contains("valid_issuer"),
            "failed_issuers should not contain 'valid_issuer'"
        );

        // Verify loading state counts
        assert_eq!(
            loader.loading_state.total_issuers(),
            2,
            "loading state should track total issuers"
        );

        assert_eq!(
            loader.loading_state.failed_issuers().len(),
            1,
            "loading state should have 1 failed issuer"
        );
    }
}
