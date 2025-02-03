// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::hash::{Hash, Hasher};
use std::sync::Arc;

use super::Token;
use chrono::{Duration, TimeDelta, Utc};
use rand::random;
use sparkv::SparKV;
use twox_hash::XxHash64;

pub struct JwtCache {
    cache: SparKV<Arc<Token>>,
    hash_seed: u64,
}

const DEFAULT_TTL: TimeDelta = Duration::hours(1);
const MAX_SPARKV_TTL: i64 = 3600;

impl JwtCache {
    pub fn new() -> Self {
        Self {
            cache: SparKV::new(),
            hash_seed: random::<u64>(),
        }
    }

    pub fn insert(
        &mut self,
        jwt_str: &str,
        decoded_jwt: Token,
    ) -> Result<Arc<Token>, JwtCacheError> {
        let hash = hash_jwt(self.hash_seed, &decoded_jwt.name, jwt_str);
        let exp = decoded_jwt
            .get_claim("exp")
            .and_then(|x| x.value().as_i64());

        let token = Arc::new(decoded_jwt);

        let ttl = if let Some(exp) = exp {
            let now = Utc::now().timestamp();
            if exp <= now {
                return Ok(token);
            }
            // we cap the TTL to 1 hr since that's the default max TLL in SparKV
            // otherwise, we get a TTLTooLong from SparKV
            let ttl = (exp - now).min(MAX_SPARKV_TTL);
            Duration::seconds(ttl)
        } else {
            DEFAULT_TTL
        };

        self.cache.set_with_ttl(&hash, token.clone(), ttl)?;

        Ok(token)
    }

    pub fn get(&self, tkn_name: &str, jwt_str: &str) -> Option<Arc<Token>> {
        let hash = hash_jwt(self.hash_seed, tkn_name, jwt_str);
        self.cache.get(&hash).cloned()
    }
}

fn hash_jwt(seed: u64, tkn_name: &str, jwt_str: &str) -> String {
    let mut s = XxHash64::with_seed(seed);
    tkn_name.hash(&mut s);
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
        let access_tkn = Token::new(
            "access_token",
            HashMap::from([("aud".to_string(), json!("some_aud"))]).into(),
            None,
        );
        let access_tkn = cache
            .insert("some.access.tkn", access_tkn)
            .expect("should insert token into cache");

        let id_tkn = Token::new(
            "id_token",
            HashMap::from([
                ("aud".to_string(), json!("some_aud")),
                ("exp".to_string(), json!(Utc::now().timestamp())),
            ])
            .into(),
            None,
        );
        cache
            .insert("some.id.tkn", id_tkn)
            .expect("should insert token into cache");

        let userinfo_exp = json!(Utc::now().timestamp() + 300);
        let userinfo_tkn = Token::new(
            "userinfo_token",
            HashMap::from([
                ("aud".to_string(), json!("some_aud")),
                ("exp".to_string(), userinfo_exp.clone()),
            ])
            .into(),
            None,
        );
        let userinfo_tkn = cache
            .insert("some.userinfo.tkn", userinfo_tkn)
            .expect("should insert token into cache");

        // Check access token
        assert_eq!(
            cache.get("access_token", "some.access.tkn"),
            Some(access_tkn),
            "should have correct access token in cache"
        );

        // Check id token
        assert_eq!(
            cache.get("id_token", "some.id.tkn"),
            None,
            "should not have id token in the cache"
        );

        // Check userinfo token
        assert_eq!(
            cache.get("userinfo_token", "some.userinfo.tkn"),
            Some(userinfo_tkn),
            "should have correct userinfo token in cache"
        );
    }
}
