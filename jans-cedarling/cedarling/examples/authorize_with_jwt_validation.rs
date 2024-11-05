/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use cedarling::{
    BootstrapConfig, Cedarling, JwtConfig, LogConfig, LogTypeConfig, PolicyStoreConfig,
    PolicyStoreSource, Request, ResourceData,
};
use std::collections::HashMap;

static POLICY_STORE_RAW: &str =
    include_str!("../../test_files/policy-store_with_trusted_issuers_ok.json");

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Configure the JwtService to validate signatures using the specified algorithms:
    // `HS256` and `RS256`.
    //
    // Tokens signed with an algorithm not in `signature_algorithms`
    // will be automatically marked as invalid.
    let jwt_config = JwtConfig::Enabled {
        signature_algorithms: vec!["HS256".to_string(), "RS256".to_string()],
    };

    let cedarling = Cedarling::new(BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::StdOut,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Json(POLICY_STORE_RAW.to_string()),
        },
        jwt_config,
    })?;

    // access_token claims:
    // {
    //   "iss": "https://admin-ui-test.gluu.org",
    //   "aud": "some_audience",
    //   "sub": "some_subject",
    //   "exp": 2724945978, ->  May 8, 2056 01:26:18 GMT+0800
    //   "iat": 1624832259  ->  June 28, 2021 06:17:39 GMT+0800
    // }
    let access_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmciLCJhdWQiOiJzb21lX2F1ZGllbmNlIiwic3ViIjoic29tZV9zdWJqZWN0IiwiZXhwIjoyNzI0OTQ1OTc4LCJpYXQiOjE2MjQ4MzIyNTl9.oZKCdPvtvA8yJ5BQhP5725TYf0CAzcOZEhPQmom7cOc".to_string();

    // id_token claims:
    // {
    //   "iss": "https://admin-ui-test.gluu.org",
    //   "aud": "some_audience",
    //   "sub": "some_subject",
    //   "exp": 2724945978, ->  May 8, 2056 01:26:18 GMT+0800
    //   "iat": 1624832259  ->  June 28, 2021 06:17:39 GMT+0800
    //   "nonce": "123123123",
    //   "name": "Mr. admin",
    //   "email": "admin@gluu.org"
    // }
    let id_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmciLCJhdWQiOiJzb21lX2F1ZGllbmNlIiwic3ViIjoic29tZV9zdWJqZWN0IiwiZXhwIjoyNzI0OTQ1OTc4LCJpYXQiOjE2MjQ4MzIyNTksIm5vbmNlIjoiMTIzMTIzMTIzIiwibmFtZSI6Ik1yLiBhZG1pbiIsImVtYWlsIjoiYWRtaW5AZ2x1dS5vcmcifQ.Zzx3gz3d3YK2geb0aCPLyiOEvFviuMsGbf1urNnmPDU".to_string();

    // userinfo_token claims:
    // {
    //   "iss": "https://admin-ui-test.gluu.org",
    //   "aud": "some_audience",
    //   "sub": "some_subject",
    //   "exp": 2724945978, ->  May 8, 2056 01:26:18 GMT+0800
    //   "iat": 1624832259  ->  June 28, 2021 06:17:39 GMT+0800
    //   "nonce": "123123123",
    //   "name": "Mr. admin",
    //   "email": "admin@gluu.org",
    //   "email_verified": true,
    //   "locale": "en_US",
    // }
    let userinfo_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmciLCJhdWQiOiJzb21lX2F1ZGllbmNlIiwic3ViIjoic29tZV9zdWJqZWN0IiwiZXhwIjoyNzI0OTQ1OTc4LCJpYXQiOjE2MjQ4MzIyNTksIm5vbmNlIjoiMTIzMTIzMTIzIiwibmFtZSI6Ik1yLiBhZG1pbiIsImVtYWlsIjoiYWRtaW5AZ2x1dS5vcmciLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibG9jYWxlIjoiZW5fVVMifQ.HvX2s_ZWUfyvRHLUl5CWaSPOIp9zVpwP2LbF5U6tNGA".to_string();

    let result = cedarling.authorize(Request {
        access_token,
        id_token,
        userinfo_token,
        action: "Jans::Action::\"Update\"".to_string(),
        context: serde_json::json!({}),
        resource: ResourceData {
            id: "random_id".to_string(),
            resource_type: "Jans::Issue".to_string(),
            payload: HashMap::from_iter([(
                "org_id".to_string(),
                serde_json::Value::String("some_long_id".to_string()),
            )]),
        },
    });
    if let Err(ref e) = &result {
        eprintln!("Error while authorizing: {:?}\n\n", e)
    }

    Ok(())
}
