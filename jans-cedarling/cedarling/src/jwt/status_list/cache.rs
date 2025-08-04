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
    LogLevel, LogWriter,
    jwt::{
        IssuerConfig,
        decode::{DecodeJwtError, decode_jwt},
        key_service::KeyService,
        log_entry::JwtLogEntry,
        validation::{JwtValidator, JwtValidatorCache, TokenKind, ValidatorInfo},
    },
    log::Logger,
};

use super::{StatusList, StatusListJwt, StatusListJwtStr, UpdateStatusListError};

/// The value of the `status_list_uri` claim from a JWT
pub type StatusListUri = String;

/// Contains an Arc<RwLock<_>> internally so clone should be fine
#[derive(Debug, Default, Clone)]
pub struct StatusListCache {
    pub status_lists: Arc<RwLock<HashMap<StatusListUri, StatusList>>>,
}

impl StatusListCache {
    /// Initializes the statuslist for the given issuer
    pub async fn init_for_iss(
        &mut self,
        iss_config: &IssuerConfig,
        validators: &JwtValidatorCache,
        key_service: &KeyService,
        logger: Option<Logger>,
    ) -> Result<(), UpdateStatusListError> {
        let openid_config = iss_config
            .openid_config
            .as_ref()
            .ok_or(UpdateStatusListError::MissingOpenIdConfig)?;
        let status_list_url = openid_config
            .status_list_endpoint
            .as_ref()
            .ok_or(UpdateStatusListError::MissingStatusListUri)?;
        let status_list_jwt = StatusListJwtStr::get_from_url(status_list_url)
            .await
            .map_err(UpdateStatusListError::GetStatusListJwt)?;

        let decoded_jwt = decode_jwt(&status_list_jwt.0)?;

        // Get decoding key
        let decoding_key_info = decoded_jwt.decoding_key_info();
        let decoding_key = key_service.get_key(&decoding_key_info);

        // get validator
        let validator_key = ValidatorInfo {
            iss: decoded_jwt.iss(),
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
                .validate_jwt(&status_list_jwt.0, decoding_key)?
        }
        .try_into()
        .map_err(DecodeJwtError::DeserializeClaims)?;

        let ttl = status_list_jwt.ttl;
        let status_list: StatusList = status_list_jwt.try_into()?;

        // spawn a background task to handle updating the statuslist if the JWT has a TTL
        // claim
        if let Some(ttl) = ttl {
            crate::http::spawn_task(keep_status_list_updated(
                ttl,
                status_list_url.clone(),
                decoding_key.cloned(),
                validator,
                self.status_lists.clone(),
                logger,
            ));
        }

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
async fn keep_status_list_updated(
    mut ttl: u64,
    status_list_url: Url,
    decoding_key: Option<DecodingKey>,
    validator: Arc<RwLock<JwtValidator>>,
    status_lists: Arc<RwLock<HashMap<String, StatusList>>>,
    logger: Option<Logger>,
) {
    loop {
        tokio::time::sleep(Duration::from_secs(ttl)).await;

        let status_list_jwt = match StatusListJwtStr::get_from_url(&status_list_url).await {
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
                .validate_jwt(&status_list_jwt.0, decoding_key.as_ref())
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
        // if the TTL is disappears somehow, we will default to 10 mins
        // otherwise, just update it to the new value
        ttl = status_list_jwt.ttl.unwrap_or(600);

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
            } else {
                logger.log_any(JwtLogEntry::new(
                    format!(
                        "missing entry for '{}' in the status list cache, will no longer keep this entry updated",
                        status_list_url.as_str(),
                    ),
                    Some(LogLevel::ERROR),
                ));
                return;
            };
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
    use crate::JwtConfig;
    use crate::common::policy_store::TrustedIssuer;
    use crate::jwt::test_utils::MockServer;
    use std::collections::HashSet;
    use std::time::Duration;

    #[tokio::test]
    async fn keep_status_list_updated() {
        // Setup
        let mut validators = JwtValidatorCache::default();
        let mut key_service = KeyService::default();
        let mut mock_server = MockServer::new_with_defaults().await.unwrap();
        key_service
            .get_keys_using_oidc(&mock_server.openid_config(), &None)
            .await
            .unwrap();
        // we initialize the status list with a 1 sec ttl
        mock_server.generate_status_list_endpoint(1u8.try_into().unwrap(), &[0b1111_1110], Some(1));
        let mut status_list = StatusListCache::default();
        let iss_config = IssuerConfig {
            issuer_id: "some_iss_id".into(),
            policy: Arc::new(TrustedIssuer {
                name: "some_iss".into(),
                description: "is a trusted issuer".into(),
                oidc_endpoint: mock_server.openid_config_endpoint().unwrap(),
                token_metadata: Default::default(),
            }),
            openid_config: Some(mock_server.openid_config()),
        };
        validators.init_for_iss(
            &iss_config,
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: false,
                jwt_status_validation: true,
                signature_algorithms_supported: HashSet::from([Algorithm::HS256]),
            },
            &status_list,
            None,
        );
        status_list
            .init_for_iss(&iss_config, &validators, &key_service, None)
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
}
