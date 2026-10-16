// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Sigstore check of a `.cjar` policy store, run on the archive bytes before they are parsed.

use std::fmt::Write as _;
use std::io::Read as _;

use sha2::{Digest, Sha256};
use sigstore_verifier::{
    IdentityMatch, SigstoreBlobVerifier, SigstoreVerificationError, VerificationPolicy,
};

use crate::bootstrap_config::PolicyStoreSignature;
use crate::http::HttpClient;

/// Bundles are a few KB; anything near this is not a bundle.
const MAX_BUNDLE_BYTES: u64 = 1024 * 1024;

/// Why a policy store failed its signature check.
#[derive(Debug, thiserror::Error)]
pub enum SignatureError {
    /// The bundle could not be fetched or read.
    #[error("failed to load the policy store signature bundle from {location}: {reason}")]
    Bundle { location: String, reason: String },
    /// An in-memory archive has no location to derive its bundle location from.
    #[error(
        "no signature bundle location for an in-memory policy store; set CEDARLING_POLICY_STORE_SIGSTORE_BUNDLE_URI"
    )]
    NoBundleLocation,
    /// The bundle does not verify the archive for the configured signer.
    #[error(
        "policy store signature does not verify for identity '{identity}' and issuer '{issuer}': {source}"
    )]
    Verification {
        identity: String,
        issuer: String,
        #[source]
        source: SigstoreVerificationError,
    },
}

/// Who signed a verified archive, and its digest.
#[derive(Debug)]
pub(crate) struct VerifiedArchive {
    pub identity: String,
    pub issuer: String,
    /// Rekor `integratedTime`, UNIX seconds.
    pub integrated_time: i64,
    /// Lowercase hex SHA-256 of the archive bytes.
    pub sha256: String,
}

impl std::fmt::Display for VerifiedArchive {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(
            f,
            "identity={} issuer={} archive_sha256={} rekor_integrated_time={}",
            self.identity, self.issuer, self.sha256, self.integrated_time
        )
    }
}

/// Runs [`ArchiveVerifier::check`] when signature checking is enabled.
pub(crate) async fn check_if_enabled(
    verifier: Option<&ArchiveVerifier>,
    http: &HttpClient,
    store_location: Option<&str>,
    archive: &[u8],
) -> Result<Option<VerifiedArchive>, SignatureError> {
    match verifier {
        Some(verifier) => verifier
            .check(http, store_location, archive)
            .await
            .map(Some),
        None => Ok(None),
    }
}

/// Verifies `.cjar` archives against the configured signer.
pub(crate) struct ArchiveVerifier {
    verifier: SigstoreBlobVerifier,
    identity: String,
    issuer: String,
    bundle_uri: Option<String>,
}

impl ArchiveVerifier {
    /// Returns `None` when signature checking is disabled.
    pub(crate) fn new(signature: &PolicyStoreSignature) -> Option<Self> {
        if *signature == PolicyStoreSignature::Disabled {
            return None;
        }
        Self::with_verifier(signature, SigstoreBlobVerifier::with_static_trust_root())
    }

    pub(crate) fn with_verifier(
        signature: &PolicyStoreSignature,
        verifier: SigstoreBlobVerifier,
    ) -> Option<Self> {
        let PolicyStoreSignature::Enforce {
            cert_identity,
            cert_oidc_issuer,
            bundle_uri,
        } = signature
        else {
            return None;
        };
        Some(Self {
            verifier,
            identity: cert_identity.clone(),
            issuer: cert_oidc_issuer.clone(),
            bundle_uri: bundle_uri.clone(),
        })
    }

    /// Loads the bundle for a store at `store_location` and verifies `archive` with it.
    /// `store_location` is `None` for in-memory archives.
    pub(crate) async fn check(
        &self,
        http: &HttpClient,
        store_location: Option<&str>,
        archive: &[u8],
    ) -> Result<VerifiedArchive, SignatureError> {
        let location = self
            .bundle_location(store_location)
            .ok_or(SignatureError::NoBundleLocation)?;
        let bundle = self.load_bundle(http, &location).await?;
        self.verify(archive, &bundle)
    }

    /// The configured override, else the store location plus `.sigstore.json`.
    fn bundle_location(&self, store_location: Option<&str>) -> Option<String> {
        self.bundle_uri
            .clone()
            .or_else(|| store_location.map(default_bundle_location))
    }

    /// Reads the bundle at `location`, over HTTP for `http(s)` URLs, else from disk.
    async fn load_bundle(
        &self,
        http: &HttpClient,
        location: &str,
    ) -> Result<Vec<u8>, SignatureError> {
        let bundle = if is_http_url(location) {
            fetch_bundle(http, location).await
        } else {
            read_bundle_file(location)
        };
        bundle.map_err(|reason| SignatureError::Bundle {
            location: location.to_string(),
            reason,
        })
    }

    /// Verifies `bundle` over `archive`.
    fn verify(&self, archive: &[u8], bundle: &[u8]) -> Result<VerifiedArchive, SignatureError> {
        let policy = VerificationPolicy {
            cert_identity: IdentityMatch::Exact(self.identity.clone()),
            cert_issuer: self.issuer.clone(),
        };
        let verified = self
            .verifier
            .verify(archive, bundle, &policy)
            .map_err(|source| SignatureError::Verification {
                identity: self.identity.clone(),
                issuer: self.issuer.clone(),
                source,
            })?;
        Ok(VerifiedArchive {
            identity: verified.subject_alternative_name,
            issuer: verified.issuer,
            integrated_time: verified.verified_at,
            sha256: sha256_hex(archive),
        })
    }
}

fn is_http_url(location: &str) -> bool {
    url::Url::parse(location).is_ok_and(|url| matches!(url.scheme(), "http" | "https"))
}

/// Appends `.sigstore.json` to the path, so a URL's query string stays at the end.
fn default_bundle_location(store_location: &str) -> String {
    match url::Url::parse(store_location) {
        Ok(mut url) if matches!(url.scheme(), "http" | "https") => {
            let path = format!("{}.sigstore.json", url.path());
            url.set_path(&path);
            url.into()
        },
        _ => format!("{store_location}.sigstore.json"),
    }
}

async fn fetch_bundle(http: &HttpClient, url: &str) -> Result<Vec<u8>, String> {
    let response = http.get_with_retry(url).await.map_err(|e| e.to_string())?;
    crate::http_utils::read_response_capped(response, Some(MAX_BUNDLE_BYTES))
        .await
        .map_err(|e| e.to_string())
}

fn read_bundle_file(path: &str) -> Result<Vec<u8>, String> {
    let mut bundle = Vec::new();
    std::fs::File::open(path)
        .and_then(|file| file.take(MAX_BUNDLE_BYTES + 1).read_to_end(&mut bundle))
        .map_err(|e| e.to_string())?;
    if bundle.len() as u64 > MAX_BUNDLE_BYTES {
        return Err(format!("bundle exceeds {MAX_BUNDLE_BYTES} bytes"));
    }
    Ok(bundle)
}

fn sha256_hex(bytes: &[u8]) -> String {
    Sha256::digest(bytes)
        .iter()
        .fold(String::with_capacity(64), |mut hex, byte| {
            let _ = write!(hex, "{byte:02x}");
            hex
        })
}

#[cfg(test)]
mod tests {
    use std::time::Duration;

    use mockito::Server;
    use sigstore_verifier::testing::TestSigner;

    use super::*;
    use crate::http::HttpClientConfig;

    const ARCHIVE: &[u8] = b"abc";
    const ARCHIVE_SHA256: &str = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    fn enforce(identity: &str) -> PolicyStoreSignature {
        PolicyStoreSignature::Enforce {
            cert_identity: identity.to_string(),
            cert_oidc_issuer: TestSigner::ISSUER.to_string(),
            bundle_uri: None,
        }
    }

    fn verifier_for(signer: &TestSigner, identity: &str) -> ArchiveVerifier {
        let verifier = SigstoreBlobVerifier::new(&signer.trust_root()).expect("test trust root");
        ArchiveVerifier::with_verifier(&enforce(identity), verifier).expect("enforce builds")
    }

    fn http_client() -> HttpClient {
        HttpClient::new(HttpClientConfig {
            max_retries: 0,
            retry_delay: Duration::from_millis(1),
            request_timeout: Duration::from_secs(2),
            max_response_size_bytes: None,
        })
        .expect("http client")
    }

    #[test]
    fn disabled_builds_no_verifier() {
        assert!(
            ArchiveVerifier::new(&PolicyStoreSignature::Disabled).is_none(),
            "disabled mode must not verify"
        );
    }

    #[test]
    fn valid_bundle_verifies_and_reports_signer_and_digest() {
        let signer = TestSigner::new();
        let verified = verifier_for(&signer, TestSigner::IDENTITY)
            .verify(ARCHIVE, &signer.sign(ARCHIVE))
            .expect("bundle from the expected signer must verify");

        assert_eq!(verified.identity, TestSigner::IDENTITY, "signer identity");
        assert_eq!(verified.issuer, TestSigner::ISSUER, "signer issuer");
        assert_eq!(
            verified.integrated_time,
            TestSigner::INTEGRATED_TIME,
            "Rekor time"
        );
        assert_eq!(verified.sha256, ARCHIVE_SHA256, "archive digest");
    }

    #[test]
    fn modified_archive_is_rejected() {
        let signer = TestSigner::new();
        let err = verifier_for(&signer, TestSigner::IDENTITY)
            .verify(b"abd", &signer.sign(ARCHIVE))
            .expect_err("a changed archive must not verify");
        assert!(
            matches!(
                err,
                SignatureError::Verification {
                    source: SigstoreVerificationError::SignatureMismatch { .. },
                    ..
                }
            ),
            "expected a signature mismatch, got {err:?}"
        );
    }

    #[test]
    fn wrong_identity_is_rejected_with_expected_signer_in_message() {
        let signer = TestSigner::new();
        let err = verifier_for(&signer, "someone-else@example.com")
            .verify(ARCHIVE, &signer.sign(ARCHIVE))
            .expect_err("an unexpected signer must not verify");
        assert!(
            err.to_string().contains("someone-else@example.com"),
            "error must name the expected identity, got: {err}"
        );
    }

    #[test]
    fn static_trust_root_rejects_test_bundles() {
        let signer = TestSigner::new();
        ArchiveVerifier::new(&enforce(TestSigner::IDENTITY))
            .expect("enforce builds")
            .verify(ARCHIVE, &signer.sign(ARCHIVE))
            .expect_err("production trust root must not trust the test signer");
    }

    #[test]
    fn verified_archive_displays_signer_and_digest() {
        let verified = VerifiedArchive {
            identity: "signer@example.com".to_string(),
            issuer: "https://accounts.google.com".to_string(),
            integrated_time: 1_792_033_521,
            sha256: ARCHIVE_SHA256.to_string(),
        };
        assert_eq!(
            verified.to_string(),
            format!(
                "identity=signer@example.com issuer=https://accounts.google.com \
                 archive_sha256={ARCHIVE_SHA256} rekor_integrated_time=1792033521"
            ),
            "log text names the signer, issuer, digest and Rekor time"
        );
    }

    #[test]
    fn default_bundle_location_appends_to_path() {
        let cases = [
            (
                "https://example.com/store.cjar",
                "https://example.com/store.cjar.sigstore.json",
            ),
            (
                "https://example.com/store.cjar?v=2",
                "https://example.com/store.cjar.sigstore.json?v=2",
            ),
            (
                "/etc/cedarling/store.cjar",
                "/etc/cedarling/store.cjar.sigstore.json",
            ),
            ("store.cjar", "store.cjar.sigstore.json"),
        ];
        for (store, bundle) in cases {
            assert_eq!(default_bundle_location(store), bundle, "bundle for {store}");
        }
    }

    #[test]
    fn bundle_uri_override_wins() {
        let signature = PolicyStoreSignature::Enforce {
            cert_identity: TestSigner::IDENTITY.to_string(),
            cert_oidc_issuer: TestSigner::ISSUER.to_string(),
            bundle_uri: Some("https://lock.example.com/policy/x/sigstore".to_string()),
        };
        let verifier = ArchiveVerifier::new(&signature).expect("enforce builds");
        assert_eq!(
            verifier
                .bundle_location(Some("https://lock.example.com/policy/x"))
                .as_deref(),
            Some("https://lock.example.com/policy/x/sigstore"),
            "configured bundle URI must be used as is"
        );
        assert_eq!(
            verifier.bundle_location(None).as_deref(),
            Some("https://lock.example.com/policy/x/sigstore"),
            "in-memory archives use the configured bundle URI"
        );
    }

    #[tokio::test]
    async fn in_memory_archive_without_bundle_uri_is_rejected() {
        let verifier = ArchiveVerifier::new(&enforce(TestSigner::IDENTITY)).expect("enforce");
        let err = verifier
            .check(&http_client(), None, ARCHIVE)
            .await
            .expect_err("no store location and no override leaves no bundle to check");
        assert!(
            matches!(err, SignatureError::NoBundleLocation),
            "expected NoBundleLocation, got {err:?}"
        );
    }

    #[tokio::test]
    async fn bundle_loads_from_http_and_rejects_missing() {
        let mut server = Server::new_async().await;
        let ok = server
            .mock("GET", "/store.cjar.sigstore.json")
            .with_status(200)
            .with_body("{}")
            .create_async()
            .await;
        let missing = server
            .mock("GET", "/missing.sigstore.json")
            .with_status(404)
            .create_async()
            .await;
        let verifier = ArchiveVerifier::new(&enforce(TestSigner::IDENTITY)).expect("enforce");
        let http = http_client();

        let bundle = verifier
            .load_bundle(&http, &format!("{}/store.cjar.sigstore.json", server.url()))
            .await
            .expect("bundle served with 200 must load");
        assert_eq!(bundle, b"{}", "bundle body");

        let err = verifier
            .load_bundle(&http, &format!("{}/missing.sigstore.json", server.url()))
            .await
            .expect_err("a 404 bundle must fail");
        assert!(
            matches!(err, SignatureError::Bundle { .. }),
            "expected a bundle error, got {err:?}"
        );
        ok.assert_async().await;
        missing.assert_async().await;
    }

    #[tokio::test]
    async fn bundle_loads_from_file_and_rejects_missing_or_oversized() {
        let dir = tempfile::tempdir().expect("temp dir");
        let verifier = ArchiveVerifier::new(&enforce(TestSigner::IDENTITY)).expect("enforce");
        let http = http_client();

        let path = dir.path().join("store.cjar.sigstore.json");
        std::fs::write(&path, b"{}").expect("write bundle");
        let bundle = verifier
            .load_bundle(&http, path.to_str().expect("utf-8 path"))
            .await
            .expect("bundle file must load");
        assert_eq!(bundle, b"{}", "bundle file contents");

        let missing = dir.path().join("missing.sigstore.json");
        verifier
            .load_bundle(&http, missing.to_str().expect("utf-8 path"))
            .await
            .expect_err("a missing bundle file must fail");

        let big = dir.path().join("big.sigstore.json");
        let size = usize::try_from(MAX_BUNDLE_BYTES).expect("fits usize") + 1;
        std::fs::write(&big, vec![b' '; size]).expect("write big bundle");
        verifier
            .load_bundle(&http, big.to_str().expect("utf-8 path"))
            .await
            .expect_err("an oversized bundle file must fail");
    }
}
