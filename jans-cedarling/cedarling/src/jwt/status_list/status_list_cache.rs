// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::{
    collections::HashMap,
    sync::{Arc, RwLock},
};

use crate::jwt::{
    decode::decode_jwt, http_utils::GetFromUrl, key_service::KeyService, validation::{JwtValidatorCache, TokenKind, ValidatorInfo}, IssuerConfig
};

use super::{StatusList, StatusListJwtStr, UpdateStatusListError};

/// The value of the `status_list_uri` claim from a JWT
pub type StatusListUri = String;

/// Contains an Arc<RwLock<_>> internally so clone should be fine
#[derive(Debug, Default, Clone)]
pub struct StatusListCache {
    status_lists: Arc<RwLock<HashMap<StatusListUri, StatusList>>>,
}

impl StatusListCache {
    pub fn lists(&self) -> Arc<RwLock<HashMap<StatusListUri, StatusList>>> {
        self.status_lists.clone()
    }
}

impl StatusListCache {
    /// Initializes the statuslist for the given issuer
    pub async fn init_for_iss(
        &mut self,
        iss_config: &IssuerConfig,
        validators: &JwtValidatorCache,
        key_service: &KeyService,
    ) -> Result<(), UpdateStatusListError> {
        let openid_config = iss_config
            .openid_config
            .as_ref()
            .expect("TODO: handle this error");
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

        let status_list_jwt = {
            validator
                .read()
                .expect("acquire JwtValidator read lock")
                .validate_jwt(&status_list_jwt.0, decoding_key)?
        };
        let status_list: StatusList = status_list_jwt.try_into()?;

        self.status_lists
            .write()
            .as_mut()
            .expect("acquire status lists write lock")
            .insert(status_list_url.to_string(), status_list);

        Ok(())
    }
}

impl From<HashMap<String, StatusList>> for StatusListCache {
    fn from(status_lists: HashMap<String, StatusList>) -> Self {
        Self {
            status_lists: Arc::new(RwLock::new(status_lists)),
        }
    }
}
