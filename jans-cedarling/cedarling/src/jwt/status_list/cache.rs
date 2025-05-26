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
    LogWriter,
    jwt::{
        IssuerConfig,
        decode::{DecodeJwtError, decode_jwt},
        http_utils::GetFromUrl,
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
    status_lists: HashMap<StatusListUri, Arc<RwLock<StatusList>>>,
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
        let status_list_jwt = StatusListJwtStr::get_from_url(status_list_url).await?;

        let decoded_jwt = decode_jwt(&status_list_jwt.0)?;

        // Get decoding key
        let decoding_key_info = decoded_jwt.decoding_key_info();
        let decoding_key = key_service.get_key(&decoding_key_info).ok_or(
            UpdateStatusListError::MissingValidationKey(status_list_url.to_string()),
        )?;

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

        let ttl = status_list_jwt.ttl.clone();
        let status_list: StatusList = status_list_jwt.try_into()?;
        let status_list = Arc::new(RwLock::new(status_list));

        // spawn a backgroud task to handle updating the statuslist if the JWT has a TTL
        // claim
        if let Some(ttl) = ttl {
            tokio::spawn(keep_status_list_updated(
                ttl,
                status_list_url.clone(),
                decoding_key.clone(),
                validator,
                status_list.clone(),
                logger,
            ));
        }

        self.status_lists
            .insert(status_list_url.to_string(), status_list);

        Ok(())
    }

    /// Returns the statuslist for the Given URI
    pub fn get(&self, uri: &str) -> Option<Arc<RwLock<StatusList>>> {
        self.status_lists.get(uri).cloned()
    }
}

/// Keeps the statuslist form the given URL updated based on the TTL
async fn keep_status_list_updated(
    mut ttl: u64,
    status_list_url: Url,
    decoding_key: DecodingKey,
    validator: Arc<RwLock<JwtValidator>>,
    status_list: Arc<RwLock<StatusList>>,
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
                    Some(crate::LogLevel::ERROR),
                ));
                continue;
            },
        };

        let validated_jwt = match {
            validator
                .read()
                .expect("acquire JwtValidator read lock")
                .validate_jwt(&status_list_jwt.0, &decoding_key)
        } {
            Ok(validated_jwt) => validated_jwt,
            Err(e) => {
                logger.log_any(JwtLogEntry::new(
                    format!(
                        "failed to validate an updated the status list JWT from '{0}': {1}",
                        status_list_url.as_str(),
                        e
                    ),
                    Some(crate::LogLevel::ERROR),
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
                    Some(crate::LogLevel::ERROR),
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
                    Some(crate::LogLevel::ERROR),
                ));
                continue;
            },
        };

        {
            let mut lock = status_list.write().expect("obtain status list write lock");
            *lock = updated_status_list;
        }
    }
}

impl From<HashMap<String, StatusList>> for StatusListCache {
    fn from(status_lists: HashMap<String, StatusList>) -> Self {
        Self {
            status_lists: status_lists
                .into_iter()
                .map(|(uri, status_list)| (uri, Arc::new(RwLock::new(status_list))))
                .collect(),
        }
    }
}

// we cannot derive PartialEqAutomaticall because of the Arc<RwLock<_>> in the
// `status_lists` so we implement it manually.
impl PartialEq for StatusListCache {
    fn eq(&self, other: &Self) -> bool {
        let self_status_lists = self
            .status_lists
            .clone()
            .into_iter()
            .map(|(uri, status_list)| {
                let status_list = status_list.read().unwrap();
                (uri, status_list.clone())
            })
            .collect::<HashMap<String, StatusList>>();
        let other_status_lists = other
            .status_lists
            .clone()
            .into_iter()
            .map(|(uri, status_list)| {
                let status_list = status_list.read().unwrap();
                (uri, status_list.clone())
            })
            .collect::<HashMap<String, StatusList>>();
        self_status_lists == other_status_lists
    }
}

#[cfg(test)]
mod test {
    #[test]
    fn keep_status_list_updated() {
        todo!()
    }
}
