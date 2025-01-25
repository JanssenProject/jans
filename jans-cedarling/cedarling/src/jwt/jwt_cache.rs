// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::Token;
use chrono::{Duration, TimeDelta, Utc};
use serde_json::Value;
use sparkv::SparKV;
use std::{
    collections::HashMap,
    hash::{DefaultHasher, Hash, Hasher},
};

pub struct JwtCache(SparKV<HashMap<String, Value>>);

const DEFAULT_TTL: TimeDelta = Duration::hours(1);

impl JwtCache {
    pub fn new() -> Self {
        Self(SparKV::new())
    }

    pub fn insert(&mut self, jwt_str: &str, decoded_jwt: &Token<'_>) -> Result<(), JwtCacheError> {
        let hash = hash_jwt(jwt_str);
        let exp = decoded_jwt
            .get_claim("exp")
            .and_then(|x| x.value().as_i64());

        let ttl = if let Some(exp) = exp {
            let now = Utc::now().timestamp();
            if exp <= now {
                return Ok(());
            }
            // we cap the TTL to 1 hr since that's the default max TLL in SparKV
            // otherwise, we get a TTLTooLong from SparKV
            let ttl = (exp - now).min(3600);
            Duration::seconds(ttl)
        } else {
            DEFAULT_TTL
        };

        self.0
            .set_with_ttl(&hash, decoded_jwt.claims_value().clone(), ttl)?;

        Ok(())
    }

    pub fn get(&self, jwt_str: &str) -> Option<&HashMap<String, Value>> {
        let hash = hash_jwt(jwt_str);
        self.0.get(&hash)
    }
}

fn hash_jwt(jwt_str: &str) -> String {
    let mut s = DefaultHasher::new();
    jwt_str.hash(&mut s);
    format!("{:x}", s.finish())
}

#[derive(Debug, thiserror::Error)]
pub enum JwtCacheError {
    #[error("failed to write to the jwt cache: {0}")]
    WriteToCache(#[from] sparkv::Error),
}

#[cfg(test)]
mod test {
    use super::JwtCache;
    use crate::jwt::Token;
    use chrono::Utc;
    use serde_json::json;
    use std::collections::HashMap;

    #[test]
    fn test_cache() {
        let mut cache = JwtCache::new();
        let access_tkn = Token::new_access(
            HashMap::from([("aud".to_string(), json!("some_aud"))]).into(),
            None,
        );
        cache
            .insert("some.access.tkn", &access_tkn)
            .expect("should insert token into cache");

        let id_tkn = Token::new_id(
            HashMap::from([
                ("aud".to_string(), json!("some_aud")),
                ("exp".to_string(), json!(Utc::now().timestamp())),
            ])
            .into(),
            None,
        );
        cache
            .insert("some.id.tkn", &id_tkn)
            .expect("should insert token into cache");

        let userinfo_exp = json!(Utc::now().timestamp() + 300);
        let userinfo_tkn = Token::new_userinfo(
            HashMap::from([
                ("aud".to_string(), json!("some_aud")),
                ("exp".to_string(), userinfo_exp.clone()),
            ])
            .into(),
            None,
        );
        cache
            .insert("some.userinfo.tkn", &userinfo_tkn)
            .expect("should insert token into cache");

        // Check access token
        assert_eq!(
            cache.get("some.access.tkn"),
            Some(access_tkn.claims_value()),
            "should have correct access token in cache"
        );

        // Check id token
        assert_eq!(
            cache.get("some.id.tkn"),
            None,
            "should not have id token in the cache"
        );

        // Check userinfo token
        assert_eq!(
            cache.get("some.userinfo.tkn"),
            Some(userinfo_tkn.claims_value()),
            "should have correct userinfo token in cache"
        );
    }
}
