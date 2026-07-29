// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::{
    collections::HashMap,
    sync::{Arc, RwLock},
    time::Duration,
};

use jsonwebtoken::DecodingKey;
use url::Url;

use crate::{
    async_sleep::sleep,
    http::HttpClient,
    jwt::{
        decode::{decode_jwt, DecodeJwtError},
        key_service::KeyService,
        log_entry::JwtLogEntry,
        token_cache::IndexKey,
        validation::{JwtValidator, JwtValidatorCache, TokenKind, ValidatorInfo},
        IssuerConfig, TokenCache,
    },
    log::Logger,
    LogLevel, LogWriter,
};

use super::{StatusList, StatusListJwt, StatusListJwtStr, UpdateStatusListError};

/// The value of the `status_list_uri` claim from a JWT
type StatusListUri = String;

struct StatusListUpdateCtx {
    ttl: Duration,
    /// Upper bound on the refresh interval. The Status List JWT's `ttl`
    /// claim is honored when present, but capped to this value; when the JWT omits
    /// `ttl`, this value is used directly.
    refresh_interval_max: Duration,
    status_list_url: Url,
    decoding_key: Option<Arc<DecodingKey>>,
    validator: Arc<JwtValidator>,
    status_lists: Arc<RwLock<HashMap<String, StatusList>>>,
    logger: Option<Logger>,
    http_client: HttpClient,
}

pub(crate) struct InitForIssArgs<'a> {
    pub validators: &'a JwtValidatorCache,
    pub key_service: &'a KeyService,
    pub token_cache: TokenCache,
    pub logger: Option<Logger>,
    pub http_client: HttpClient,
    pub refresh_interval_max: Duration,
}

/// Resolve effective refresh interval as `min(jwt_ttl, refresh_interval_max)`,
/// or `refresh_interval_max` when the JWT omits `ttl`.
fn effective_refresh_interval(jwt_ttl_secs: Option<u64>, max: Duration) -> Duration {
    match jwt_ttl_secs {
        Some(t) => Duration::from_secs(t).min(max),
        None => max,
    }
}

/// Contains an `Arc<RwLock<_>>` internally so clone should be fine
#[derive(Debug, Default, Clone)]
pub(crate) struct StatusListCache {
    pub status_lists: Arc<RwLock<HashMap<StatusListUri, StatusList>>>,
}

impl StatusListCache {
    /// Initializes the statuslist for the given issuer
    ///
    /// If the issuer's `OpenID` configuration does not include a
    /// `status_list_endpoint`, the initialization is silently skipped with a
    /// WARN-level log message rather than failing.
    pub(crate) async fn init_for_iss(
        &self,
        iss_config: &IssuerConfig,
        args: InitForIssArgs<'_>,
    ) -> Result<(), UpdateStatusListError> {
        let InitForIssArgs {
            validators,
            key_service,
            token_cache,
            logger,
            http_client,
            refresh_interval_max,
        } = args;
        let Some(openid_config) = iss_config.openid_config.as_ref() else {
            if let Some(logger) = &logger {
                logger.log_any(JwtLogEntry::new(
                    "issuer has no OpenID configuration; status validation skipped".into(),
                    Some(LogLevel::WARN),
                ));
            }
            return Ok(());
        };
        let Some(status_list_url) = openid_config.status_list_endpoint.as_ref() else {
            if let Some(logger) = &logger {
                logger.log_any(JwtLogEntry::new(
                    format!(
                        "issuer '{}' does not publish a status_list_endpoint; \
                         status validation skipped",
                        openid_config.issuer,
                    ),
                    Some(LogLevel::WARN),
                ));
            }
            return Ok(());
        };
        let status_list_jwt = StatusListJwtStr::get_from_url(status_list_url, &http_client)
            .await
            .map_err(UpdateStatusListError::GetStatusListJwt)?;
        let iss = iss_config.policy.iss_claim();

        let decoded_jwt = decode_jwt(&status_list_jwt.0)?;

        // Get decoding key
        let decoding_key_info = decoded_jwt.decoding_key_info();
        let decoding_key = key_service.get_key(&decoding_key_info);

        let decoded_jwt_iss = decoded_jwt.iss();
        // get validator
        let validator_key = ValidatorInfo {
            iss: decoded_jwt_iss.as_ref(),
            token_kind: TokenKind::StatusList,
            algorithm: decoded_jwt.header.alg,
        };
        let validator =
            validators
                .get(&validator_key)
                .ok_or(UpdateStatusListError::MissingValidator(
                    status_list_url.to_string(),
                ))?;

        let status_list_jwt: StatusListJwt = validator
            .validate_jwt(&status_list_jwt.0, decoding_key.clone())?
            .try_into()
            .map_err(DecodeJwtError::DeserializeClaims)?;

        // Per the IETF `oauth-status-list` spec, the JWT's `ttl` claim drives the
        // refresh cadence, but the bootstrap
        // `CEDARLING_JWT_STATUS_LIST_REFRESH_INTERVAL_MAX` caps it: the issuer can
        // always request a *more frequent* refresh, but never a less frequent one.
        // When the JWT omits `ttl`, the max is used directly so the cache never goes
        // stale forever.
        let ttl = effective_refresh_interval(status_list_jwt.ttl, refresh_interval_max);
        let status_list: StatusList = status_list_jwt.try_into()?;

        let ctx = StatusListUpdateCtx {
            ttl,
            refresh_interval_max,
            status_list_url: status_list_url.clone(),
            decoding_key,
            validator,
            status_lists: self.status_lists.clone(),
            logger,
            http_client: http_client.clone(),
        };
        crate::http::spawn_task(keep_status_list_updated(
            // callback is called on updated status list
            move || {
                token_cache.invalidate_by_index(&IndexKey::Iss(iss.clone()));
            },
            ctx,
        ));

        {
            let mut status_lists = self
                .status_lists
                .write()
                .expect("acquire status_lists write lock");
            status_lists.insert(status_list_url.to_string(), status_list);
        }

        Ok(())
    }
}

/// Keeps the statuslist form the given URL updated based on the TTL
/// `cb` will be called when status list updated
async fn keep_status_list_updated<F>(cb: F, ctx: StatusListUpdateCtx)
where
    F: Fn(),
{
    let StatusListUpdateCtx {
        mut ttl,
        refresh_interval_max,
        status_list_url,
        decoding_key,
        validator,
        status_lists,
        logger,
        http_client,
    } = ctx;

    // If a refresh fails, drop the cached list instead of serving a stale one.
    // Tokens that need it then get rejected until a refresh works again. We also
    // clear the token cache so a token already cached as valid gets re-checked.
    let fail_closed = || {
        let removed = {
            let mut lists = status_lists.write().expect("obtain status list write lock");
            lists.remove(status_list_url.as_str()).is_some()
        };
        if removed {
            logger.log_any(JwtLogEntry::new(
                format!(
                    "dropped the cached status list for '{0}' after a failed refresh; \
                     tokens that reference it will be rejected until it refreshes successfully",
                    status_list_url.as_str(),
                ),
                Some(LogLevel::WARN),
            ));
            cb();
        }
    };

    loop {
        sleep(ttl).await;

        let status_list_jwt =
            match StatusListJwtStr::get_from_url(&status_list_url, &http_client).await {
                Ok(jwt) => jwt,
                Err(e) => {
                    logger.log_any(JwtLogEntry::new(
                        format!(
                            "failed to fetch an updated the status list from '{0}': {1}",
                            status_list_url.as_str(),
                            e
                        ),
                        Some(LogLevel::ERROR),
                    ));
                    fail_closed();
                    continue;
                },
            };

        let result = validator.validate_jwt(&status_list_jwt.0, decoding_key.clone());
        let validated_jwt = match result {
            Ok(validated_jwt) => validated_jwt,
            Err(e) => {
                logger.log_any(JwtLogEntry::new(
                    format!(
                        "failed to validate an updated the status list JWT from '{0}': {1}",
                        status_list_url.as_str(),
                        e
                    ),
                    Some(LogLevel::ERROR),
                ));
                fail_closed();
                continue;
            },
        };

        let status_list_jwt: StatusListJwt = match validated_jwt.try_into() {
            Ok(jwt) => jwt,
            Err(e) => {
                logger.log_any(JwtLogEntry::new(
                    format!(
                        "failed to deserialize an updated the status list JWT from '{0}': {1}",
                        status_list_url.as_str(),
                        e
                    ),
                    Some(LogLevel::ERROR),
                ));
                fail_closed();
                continue;
            },
        };
        // Honor the refreshed JWT's `ttl` but cap it at the configured max; if the
        // refreshed JWT no longer has `ttl`, use the max directly.
        ttl = effective_refresh_interval(status_list_jwt.ttl, refresh_interval_max);

        let updated_status_list: StatusList = match status_list_jwt.try_into() {
            Ok(status_list) => status_list,
            Err(e) => {
                logger.log_any(JwtLogEntry::new(
                    format!(
                        "failed to parse the updated the status list JWT from '{0}': {1}",
                        status_list_url.as_str(),
                        e
                    ),
                    Some(LogLevel::ERROR),
                ));
                fail_closed();
                continue;
            },
        };

        {
            let mut lists = status_lists.write().expect("obtain status list write lock");
            // insert, not update: re-creates the entry if a failed refresh dropped it
            lists.insert(status_list_url.to_string(), updated_status_list);
        }
        // call callback on updated status list
        cb();
    }
}

impl From<HashMap<String, StatusList>> for StatusListCache {
    fn from(status_lists: HashMap<String, StatusList>) -> Self {
        Self {
            status_lists: Arc::new(RwLock::new(status_lists)),
        }
    }
}

#[cfg(test)]
mod test {
    use jsonwebtoken::Algorithm;

    use super::*;
    use crate::common::policy_store::TrustedIssuer;
    use crate::http::HttpClientConfig;
    use crate::jwt::test_utils::MockServer;
    use crate::JwtConfig;
    use std::collections::HashSet;
    use std::time::Duration;

    #[tokio::test]
    async fn keep_status_list_updated() {
        let http_client = HttpClient::new(HttpClientConfig {
            max_retries: 0,
            retry_delay: Duration::from_millis(3),
            request_timeout: Duration::from_millis(500),
        max_response_size_bytes: None,
        })
        .expect("http client should be constructed");

        // Setup
        let validators = JwtValidatorCache::default();
        let key_service = KeyService::default();
        let mut mock_server = MockServer::new_with_defaults().await.unwrap();
        key_service
            .get_keys_using_oidc(&mock_server.openid_config(), None, http_client.clone())
            .await
            .unwrap();
        // we initialize the status list with a 1 sec ttl
        mock_server.generate_status_list_endpoint(1u8.try_into().unwrap(), &[0b1111_1110], Some(1));
        let status_list = StatusListCache::default();

        let ti = TrustedIssuer::new(
            "some_iss".into(),
            "is a trusted issuer".into(),
            mock_server.openid_config_endpoint().unwrap(),
            HashMap::default(),
        );

        let iss_config = IssuerConfig {
            issuer_id: "some_iss_id".into(),
            policy: Arc::new(ti),
            openid_config: Some(mock_server.openid_config()),
        };
        validators.init_for_iss(
            &iss_config,
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: false,
                jwt_status_validation: true,
                signature_algorithms_supported: HashSet::from([Algorithm::HS256]),
                ..Default::default()
            },
            &status_list,
            None,
        );
        status_list
            .init_for_iss(
                &iss_config,
                InitForIssArgs {
                    validators: &validators,
                    key_service: &key_service,
                    token_cache: TokenCache::default(),
                    logger: None,
                    http_client,
                    // Small cap so the JWT ttl (1s) is capped to a sub-second
                    // value, keeping the test fast.
                    refresh_interval_max: Duration::from_millis(200),
                },
            )
            .await
            .unwrap();

        let status_list_uri = mock_server.status_list_endpoint().unwrap().to_string();

        // Check if the status list is the same as we first generated
        {
            let lists = status_list
                .status_lists
                .read()
                .expect("obtain status_lists read lock");
            let status_list = lists
                .get(&status_list_uri)
                .expect("should have a status list");

            assert_eq!(
                *status_list,
                StatusList {
                    bit_size: 1u8.try_into().unwrap(),
                    list: vec![0b1111_1110],
                },
                "the status list is wrong",
            );
        }

        // Update the status in the server
        mock_server.generate_status_list_endpoint(1u8.try_into().unwrap(), &[0b0000_0001], None);

        // Wait for the token to expire and get updated
        tokio::time::sleep(Duration::from_millis(600)).await;

        // Check if the status list got updated
        {
            let lists = status_list
                .status_lists
                .read()
                .expect("obtain status_lists read lock");
            let status_list = lists
                .get(&status_list_uri)
                .expect("should have a status list");

            assert_eq!(
                *status_list,
                StatusList {
                    bit_size: 1u8.try_into().unwrap(),
                    list: vec![0b0000_0001],
                },
                "the status list was not updated",
            );
        }
    }

    /// When the status endpoint becomes unavailable, the background refresh
    /// fails closed: it drops the cached status list so tokens that depend on
    /// it are rejected instead of being checked against stale revocation data.
    #[tokio::test]
    async fn refresh_failure_evicts_status_list() {
        let http_client = HttpClient::new(HttpClientConfig {
            max_retries: 0,
            retry_delay: Duration::from_millis(3),
            request_timeout: Duration::from_millis(500),
            max_response_size_bytes: None,
        })
        .expect("http client should be constructed");

        let validators = JwtValidatorCache::default();
        let key_service = KeyService::default();
        let mut mock_server = MockServer::new_with_defaults().await.unwrap();
        key_service
            .get_keys_using_oidc(&mock_server.openid_config(), None, http_client.clone())
            .await
            .unwrap();
        mock_server.generate_status_list_endpoint(1u8.try_into().unwrap(), &[0b1111_1110], Some(1));
        let status_list = StatusListCache::default();

        let ti = TrustedIssuer::new(
            "some_iss".into(),
            "is a trusted issuer".into(),
            mock_server.openid_config_endpoint().unwrap(),
            HashMap::default(),
        );

        let iss_config = IssuerConfig {
            issuer_id: "some_iss_id".into(),
            policy: Arc::new(ti),
            openid_config: Some(mock_server.openid_config()),
        };
        validators.init_for_iss(
            &iss_config,
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: false,
                jwt_status_validation: true,
                signature_algorithms_supported: HashSet::from([Algorithm::HS256]),
                ..Default::default()
            },
            &status_list,
            None,
        );
        status_list
            .init_for_iss(
                &iss_config,
                InitForIssArgs {
                    validators: &validators,
                    key_service: &key_service,
                    token_cache: TokenCache::default(),
                    logger: None,
                    http_client,
                    refresh_interval_max: Duration::from_millis(200),
                },
            )
            .await
            .unwrap();

        let status_list_uri = mock_server.status_list_endpoint().unwrap().to_string();

        // init cached the list; now make the endpoint fail and let one refresh run.
        mock_server.fail_status_list_endpoint();
        tokio::time::sleep(Duration::from_millis(600)).await;

        {
            let lists = status_list
                .status_lists
                .read()
                .expect("obtain status_lists read lock");
            assert!(
                !lists.contains_key(&status_list_uri),
                "a failed refresh should drop the cached status list (fail closed)",
            );
        }
    }

    /// When the Status List JWT has no `ttl` claim, the bootstrap-config
    /// `status_list_refresh_interval_max` is used directly so the background
    /// refresh task still runs.
    #[tokio::test]
    async fn refresh_max_used_when_jwt_has_no_ttl() {
        let http_client = HttpClient::new(HttpClientConfig {
            max_retries: 0,
            retry_delay: Duration::from_millis(3),
            request_timeout: Duration::from_millis(500),
        max_response_size_bytes: None,
        })
        .expect("http client should be constructed");

        let validators = JwtValidatorCache::default();
        let key_service = KeyService::default();
        let mut mock_server = MockServer::new_with_defaults().await.unwrap();
        key_service
            .get_keys_using_oidc(&mock_server.openid_config(), None, http_client.clone())
            .await
            .unwrap();
        // initial JWT has no `ttl` claim - relies on the max to schedule a refresh
        mock_server
            .generate_status_list_endpoint_without_ttl(1u8.try_into().unwrap(), &[0b1111_1110]);
        let status_list = StatusListCache::default();

        let ti = TrustedIssuer::new(
            "some_iss".into(),
            "is a trusted issuer".into(),
            mock_server.openid_config_endpoint().unwrap(),
            HashMap::default(),
        );

        let iss_config = IssuerConfig {
            issuer_id: "some_iss_id".into(),
            policy: Arc::new(ti),
            openid_config: Some(mock_server.openid_config()),
        };
        validators.init_for_iss(
            &iss_config,
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: false,
                jwt_status_validation: true,
                signature_algorithms_supported: HashSet::from([Algorithm::HS256]),
                ..Default::default()
            },
            &status_list,
            None,
        );
        // 1-second max so the test runs fast
        status_list
            .init_for_iss(
                &iss_config,
                InitForIssArgs {
                    validators: &validators,
                    key_service: &key_service,
                    token_cache: TokenCache::default(),
                    logger: None,
                    http_client,
                    refresh_interval_max: Duration::from_millis(200),
                },
            )
            .await
            .unwrap();

        let status_list_uri = mock_server.status_list_endpoint().unwrap().to_string();

        // Update server-side list (still without ttl) before refresh fires
        mock_server
            .generate_status_list_endpoint_without_ttl(1u8.try_into().unwrap(), &[0b0000_0001]);

        tokio::time::sleep(Duration::from_millis(600)).await;

        let lists = status_list
            .status_lists
            .read()
            .expect("obtain status_lists read lock");
        let got = lists
            .get(&status_list_uri)
            .expect("should have a status list");
        assert_eq!(
            *got,
            StatusList {
                bit_size: 1u8.try_into().unwrap(),
                list: vec![0b0000_0001],
            },
            "max interval did not trigger a refresh",
        );
    }

    /// When the Status List JWT's `ttl` exceeds the bootstrap-config
    /// `status_list_refresh_interval_max`, the refresh loop must cap the sleep
    /// at the max so the cache still refreshes before the JWT's own ttl.
    #[tokio::test]
    async fn refresh_capped_when_jwt_ttl_exceeds_max() {
        let http_client = HttpClient::new(HttpClientConfig {
            max_retries: 0,
            retry_delay: Duration::from_millis(3),
            request_timeout: Duration::from_millis(500),
        max_response_size_bytes: None,
        })
        .expect("http client should be constructed");

        let validators = JwtValidatorCache::default();
        let key_service = KeyService::default();
        let mut mock_server = MockServer::new_with_defaults().await.unwrap();
        key_service
            .get_keys_using_oidc(&mock_server.openid_config(), None, http_client.clone())
            .await
            .unwrap();
        // JWT ttl = 10s, much larger than refresh_interval_max = 1s
        mock_server.generate_status_list_endpoint(
            1u8.try_into().unwrap(),
            &[0b1111_1110],
            Some(10),
        );
        let status_list = StatusListCache::default();

        let ti = TrustedIssuer::new(
            "some_iss".into(),
            "is a trusted issuer".into(),
            mock_server.openid_config_endpoint().unwrap(),
            HashMap::default(),
        );

        let iss_config = IssuerConfig {
            issuer_id: "some_iss_id".into(),
            policy: Arc::new(ti),
            openid_config: Some(mock_server.openid_config()),
        };
        validators.init_for_iss(
            &iss_config,
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: false,
                jwt_status_validation: true,
                signature_algorithms_supported: HashSet::from([Algorithm::HS256]),
                ..Default::default()
            },
            &status_list,
            None,
        );
        status_list
            .init_for_iss(
                &iss_config,
                InitForIssArgs {
                    validators: &validators,
                    key_service: &key_service,
                    token_cache: TokenCache::default(),
                    logger: None,
                    http_client,
                    refresh_interval_max: Duration::from_millis(200),
                },
            )
            .await
            .unwrap();

        let status_list_uri = mock_server.status_list_endpoint().unwrap().to_string();

        // Update the server-side list before the cap-driven refresh fires.
        mock_server.generate_status_list_endpoint(
            1u8.try_into().unwrap(),
            &[0b0000_0001],
            Some(10),
        );

        // Sleep > refresh_interval_max but < JWT ttl: refresh only fires if
        // the loop honored min(ttl, refresh_interval_max).
        tokio::time::sleep(Duration::from_millis(600)).await;

        let lists = status_list
            .status_lists
            .read()
            .expect("obtain status_lists read lock");
        let got = lists
            .get(&status_list_uri)
            .expect("should have a status list");
        assert_eq!(
            *got,
            StatusList {
                bit_size: 1u8.try_into().unwrap(),
                list: vec![0b0000_0001],
            },
            "refresh did not respect the configured cap over the JWT ttl",
        );
    }
}
