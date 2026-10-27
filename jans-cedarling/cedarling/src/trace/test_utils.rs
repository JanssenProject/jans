// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use jans_trace_core::Digest;
use serde_json::json;

use crate::TokenInput;
use crate::common::policy_store::PolicyStoreWithID;
use crate::common::policy_store::archive_handler::ArchiveLimits;
use crate::common::policy_store::loader::read_policy_store_archive_bytes;
use crate::common::policy_store::test_utils::{PolicyStoreTestBuilder, fixtures};

/// A store loaded from a `.cjar` archive and the `bundle_hash` of the archive bytes.
pub(crate) struct CjarStore {
    pub(crate) store: PolicyStoreWithID,
    pub(crate) bundle_hash: Digest,
}

/// `builder`'s archive loaded as TRACE requires.
pub(crate) fn cjar_store(builder: &PolicyStoreTestBuilder) -> CjarStore {
    let bytes = builder.build_archive().expect("test archive builds");
    let bundle_hash = jans_trace_core::inputs::compute_bundle_hash(&bytes);
    let store = read_policy_store_archive_bytes(bytes, false, ArchiveLimits::default())
        .expect("test archive parses")
        .store;
    CjarStore { store, bundle_hash }
}

/// A spec v2 `.cjar` store with one JWT issuer, so a multi-issuer decision has a
/// `bundle_hash` to record. Tokens of its issuer are not signature-checked
/// under `JwtConfig::new_without_validation`.
pub(crate) fn multi_issuer_store() -> PolicyStoreTestBuilder {
    let mut builder = fixtures::minimal_valid().with_spec_version(2).with_schema(
        r#"namespace Dolphin {
  entity TrustedIssuer = {"issuer_entity_id": Jans::Url};
  entity Access_Token = {
    token_type: String,
    jti: String,
    iss: TrustedIssuer,
    validated_at: Long,
    exp: Long,
    sub?: String,
  } tags Set<String>;
  entity Resource;
  entity Any;
  action "Read" appliesTo {
    principal: [Any],
    resource: [Resource],
    context: Context
  };
}
namespace Jans {
  type Url = {"host": String, "path": String, "protocol": String};
}
type Context = {
  tokens: {
    dolphin_access_token?: Dolphin::Access_Token,
    total_token_count: Long
  }
};
"#,
    );
    builder.extra_files.insert(
        "trusted-issuers.json".to_string(),
        json!({
            "Dolphin::Access_Token": {
                "type": "jwt",
                "issuer": "Dolphin",
                "configuration_endpoints": {
                    "configuration": "https://idp.dolphin.sea/.well-known/openid-configuration"
                }
            }
        })
        .to_string(),
    );
    builder
}

pub(crate) fn dolphin_token() -> TokenInput {
    let payload = test_utils::token_claims::generate_token_using_claims(json!({
        "iss": "https://idp.dolphin.sea",
        "jti": "dolphin-1",
        "sub": "u1",
        "exp": 2_000_000_000,
        "iat": 1_516_239_022,
    }));
    TokenInput::new("Dolphin::Access_Token".to_string(), payload)
}
