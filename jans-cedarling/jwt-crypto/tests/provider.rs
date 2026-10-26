// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

#![cfg(not(target_arch = "wasm32"))]

use std::sync::LazyLock;

use cedarling_jwt_crypto::PROVIDER;
use ed25519_dalek::pkcs8::EncodePrivateKey as _;
use jsonwebtoken::crypto::{CryptoProvider, aws_lc};
use jsonwebtoken::jwk::{EllipticCurve, Jwk, ThumbprintHash};
use jsonwebtoken::{Algorithm, DecodingKey, EncodingKey};
use rand_core_06::RngCore;
use rsa::pkcs1::EncodeRsaPrivateKey;

const MSG: &[u8] = b"header.payload";

fn random_32() -> [u8; 32] {
    let mut bytes = [0u8; 32];
    rand_core_06::OsRng.fill_bytes(&mut bytes);
    bytes
}

static RSA_DER: LazyLock<Vec<u8>> = LazyLock::new(|| {
    rsa::RsaPrivateKey::new(&mut rand_core_06::OsRng, 2048)
        .expect("rsa key")
        .to_pkcs1_der()
        .expect("pkcs1 der")
        .as_bytes()
        .to_vec()
});

fn p256_der() -> Vec<u8> {
    p256::ecdsa::SigningKey::from_slice(&random_32())
        .expect("p256 key")
        .to_pkcs8_der()
        .expect("pkcs8 der")
        .as_bytes()
        .to_vec()
}

fn p384_der() -> Vec<u8> {
    let mut scalar = [0u8; 48];
    rand_core_06::OsRng.fill_bytes(&mut scalar);
    p384::ecdsa::SigningKey::from_slice(&scalar)
        .expect("p384 key")
        .to_pkcs8_der()
        .expect("pkcs8 der")
        .as_bytes()
        .to_vec()
}

fn ed_der() -> Vec<u8> {
    ed25519_dalek::SigningKey::from_bytes(&random_32())
        .to_pkcs8_der()
        .expect("pkcs8 der")
        .as_bytes()
        .to_vec()
}

fn keys(alg: Algorithm) -> (EncodingKey, DecodingKey) {
    let encoding = match alg {
        Algorithm::HS256 | Algorithm::HS384 | Algorithm::HS512 => {
            let secret = random_32();
            return (EncodingKey::from_secret(&secret), DecodingKey::from_secret(&secret));
        },
        Algorithm::ES256 => EncodingKey::from_ec_der(&p256_der()),
        Algorithm::ES384 => EncodingKey::from_ec_der(&p384_der()),
        Algorithm::EdDSA => {
            let key = ed25519_dalek::SigningKey::from_bytes(&random_32());
            let der = key.to_pkcs8_der().expect("pkcs8 der");
            return (
                EncodingKey::from_ed_der(der.as_bytes()),
                DecodingKey::from_ed_der(key.verifying_key().as_bytes()),
            );
        },
        _ => EncodingKey::from_rsa_der(&RSA_DER),
    };
    let jwk = Jwk::from_encoding_key(&encoding, alg).expect("jwk");
    (encoding, DecodingKey::from_jwk(&jwk).expect("decoding key"))
}

fn sign(provider: &CryptoProvider, alg: Algorithm, key: &EncodingKey, msg: &[u8]) -> Vec<u8> {
    (provider.signer_factory)(&alg, key)
        .expect("signer")
        .try_sign(msg)
        .expect("signature")
}

fn verifies(
    provider: &CryptoProvider,
    alg: Algorithm,
    key: &DecodingKey,
    msg: &[u8],
    sig: &[u8],
) -> bool {
    (provider.verifier_factory)(&alg, key)
        .expect("verifier")
        .verify(msg, &sig.to_vec())
        .is_ok()
}

const ALGORITHMS: [Algorithm; 12] = [
    Algorithm::HS256,
    Algorithm::HS384,
    Algorithm::HS512,
    Algorithm::ES256,
    Algorithm::ES384,
    Algorithm::RS256,
    Algorithm::RS384,
    Algorithm::RS512,
    Algorithm::PS256,
    Algorithm::PS384,
    Algorithm::PS512,
    Algorithm::EdDSA,
];

#[test]
fn interoperates_with_aws_lc() {
    for alg in ALGORITHMS {
        let (encoding, decoding) = keys(alg);

        let ours = sign(&PROVIDER, alg, &encoding, MSG);
        assert!(verifies(&aws_lc::DEFAULT_PROVIDER, alg, &decoding, MSG, &ours), "{alg:?} ours->aws");
        assert!(verifies(&PROVIDER, alg, &decoding, MSG, &ours), "{alg:?} ours->ours");

        let theirs = sign(&aws_lc::DEFAULT_PROVIDER, alg, &encoding, MSG);
        assert!(verifies(&PROVIDER, alg, &decoding, MSG, &theirs), "{alg:?} aws->ours");
    }
}

#[test]
fn rejects_tampering() {
    for alg in ALGORITHMS {
        let (encoding, decoding) = keys(alg);
        let mut sig = sign(&PROVIDER, alg, &encoding, MSG);

        assert!(!verifies(&PROVIDER, alg, &decoding, b"header.other", &sig), "{alg:?} message");
        sig[0] ^= 1;
        assert!(!verifies(&PROVIDER, alg, &decoding, MSG, &sig), "{alg:?} signature");
        assert!(!verifies(&PROVIDER, alg, &decoding, MSG, &[]), "{alg:?} empty");
    }
}

#[test]
fn rejects_key_of_other_family() {
    let (hmac, _) = keys(Algorithm::HS256);
    let (_, ec) = keys(Algorithm::ES256);

    assert!((PROVIDER.signer_factory)(&Algorithm::RS256, &hmac).is_err());
    assert!((PROVIDER.verifier_factory)(&Algorithm::HS256, &ec).is_err());
    assert!((PROVIDER.verifier_factory)(&Algorithm::ES384, &ec).is_err());
}

#[test]
fn key_utils_match_aws_lc() {
    let ours = &PROVIDER.key_utils;
    let theirs = &aws_lc::DEFAULT_PROVIDER.key_utils;

    let rsa = (ours.rsa_pub_components_from_private_key)(&RSA_DER).expect("rsa components");
    assert_eq!(rsa, (theirs.rsa_pub_components_from_private_key)(&RSA_DER).expect("aws rsa"));

    for (alg, der) in [(Algorithm::ES256, p256_der()), (Algorithm::ES384, p384_der())] {
        let components = (ours.ec_pub_components_from_private_key)(&der, alg).expect("ec");
        let expected = (theirs.ec_pub_components_from_private_key)(&der, alg).expect("aws ec");
        assert_eq!(components.1, expected.1);
        assert_eq!(components.2, expected.2);
    }

    let ed = ed_der();
    assert_eq!(
        (ours.ed_pub_components_from_private_key)(&ed, &EllipticCurve::Ed25519).expect("ed"),
        (theirs.ed_pub_components_from_private_key)(&ed, &EllipticCurve::Ed25519).expect("aws ed"),
    );

    let hashes: [fn() -> ThumbprintHash; 3] = [
        || ThumbprintHash::SHA256,
        || ThumbprintHash::SHA384,
        || ThumbprintHash::SHA512,
    ];
    for hash in hashes {
        assert_eq!(
            (ours.compute_digest)(MSG, hash()).expect("digest"),
            (theirs.compute_digest)(MSG, hash()).expect("aws digest"),
        );
    }
}
