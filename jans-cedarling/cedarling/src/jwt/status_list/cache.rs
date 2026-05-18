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
    ttl: u64,
    /// Fallback refresh interval (seconds) used when an updated Status List JWT no
    /// longer carries a `ttl` claim.
    refresh_interval_fallback: u64,
    status_list_url: Url,
    decoding_key: Option<Arc<DecodingKey>>,
    validator: Arc<RwLock<JwtValidator>>,
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
    pub refresh_interval_fallback: u64,
}

/// Contains an `Arc<RwLock<_>>` internally so clone should be fine
#[derive(Debug, Default, Clone)]
pub(crate) struct StatusListCache {
    pub status_lists: Arc<RwLock<HashMap<StatusListUri, StatusList>>>,
}

impl StatusListCache {
    /// Initializes the statuslist for the given issuer
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
            refresh_interval_fallback,
        } = args;
        let openid_config = iss_config
            .openid_config
            .as_ref()
            .ok_or(UpdateStatusListError::MissingOpenIdConfig)?;
        let status_list_url = openid_config
            .status_list_endpoint
            .as_ref()
            .ok_or(UpdateStatusListError::MissingStatusListUri)?;
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

        let status_list_jwt: StatusListJwt = {
            validator
                .read()
                .expect("acquire JwtValidator read lock")
                .validate_jwt(&status_list_jwt.0, decoding_key.clone())?
        }
        .try_into()
        .map_err(DecodeJwtError::DeserializeClaims)?;

        // Per the IETF `oauth-status-list` spec, the JWT's `ttl` claim is authoritative
        // for refresh cadence. When the issuer omits `ttl`, fall back to the bootstrap
        // `CEDARLING_JWT_STATUS_LIST_REFRESH_INTERVAL_FALLBACK` so the cache never goes
        // stale forever.
        let ttl = status_list_jwt.ttl.unwrap_or(refresh_interval_fallback);
        let status_list: StatusList = status_list_jwt.try_into()?;

        let ctx = StatusListUpdateCtx {
            ttl,
            refresh_interval_fallback,
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
        refresh_interval_fallback,
        status_list_url,
        decoding_key,
        validator,
        status_lists,
        logger,
        http_client,
    } = ctx;

    loop {
        sleep(Duration::from_secs(ttl)).await;

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
                    continue;
                },
            };

        let result = {
            validator
                .read()
                .expect("acquire JwtValidator read lock")
                .validate_jwt(&status_list_jwt.0, decoding_key.clone())
        };
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
                continue;
            },
        };
        // If the refreshed JWT no longer has `ttl`, fall back to the configured
        // fallback interval so the loop keeps a sane cadence.
        ttl = status_list_jwt.ttl.unwrap_or(refresh_interval_fallback);

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
                continue;
            },
        };

        {
            let mut lists = status_lists.write().expect("obtain status list write lock");

            if let Some(list) = lists.get_mut(status_list_url.as_str()) {
                *list = updated_status_list;
                // call callback on updated status list
                cb();
            } else {
                logger.log_any(JwtLogEntry::new(
                    format!(
                        "missing entry for '{}' in the status list cache, will no longer keep this entry updated",
                        status_list_url.as_str(),
                    ),
                    Some(LogLevel::ERROR),
                ));
                return;
            }
        }
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
                    refresh_interval_fallback:
                        JwtConfig::DEFAULT_STATUS_LIST_REFRESH_INTERVAL_FALLBACK_SECS,
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
        tokio::time::sleep(Duration::from_secs(3)).await;

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

    /// When the Status List JWT has no `ttl` claim, the bootstrap-config
    /// `status_list_refresh_interval_fallback` should be used so that the
    /// background refresh task still runs.
    #[tokio::test]
    async fn refresh_fallback_used_when_jwt_has_no_ttl() {
        let http_client = HttpClient::new(HttpClientConfig {
            max_retries: 0,
            retry_delay: Duration::from_millis(3),
            request_timeout: Duration::from_millis(500),
        })
        .expect("http client should be constructed");

        let validators = JwtValidatorCache::default();
        let key_service = KeyService::default();
        let mut mock_server = MockServer::new_with_defaults().await.unwrap();
        key_service
            .get_keys_using_oidc(&mock_server.openid_config(), None, http_client.clone())
            .await
            .unwrap();
        // initial JWT has no `ttl` claim - relies on fallback to schedule a refresh
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
        // 1-second fallback so the test runs fast
        status_list
            .init_for_iss(
                &iss_config,
                InitForIssArgs {
                    validators: &validators,
                    key_service: &key_service,
                    token_cache: TokenCache::default(),
                    logger: None,
                    http_client,
                    refresh_interval_fallback: 1,
                },
            )
            .await
            .unwrap();

        let status_list_uri = mock_server.status_list_endpoint().unwrap().to_string();

        // Update server-side list (still without ttl) before refresh fires
        mock_server
            .generate_status_list_endpoint_without_ttl(1u8.try_into().unwrap(), &[0b0000_0001]);

        tokio::time::sleep(Duration::from_secs(3)).await;

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
            "fallback interval did not trigger a refresh",
        );
    }
}
