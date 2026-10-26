// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `jsonwebtoken` [`CryptoProvider`] built on the RustCrypto `p256`/`p384` 0.14 stack.
//!
//! `jsonwebtoken`'s own `rust_crypto` feature still pins `p256`/`p384` 0.13, which pull
//! `crypto-bigint` 0.5 (RUSTSEC-2026-0387). Targets that cannot build `aws-lc-rs`
//! (wasm32) install this provider instead.

use ed25519_dalek::pkcs8::DecodePrivateKey as _;
use hmac::{Hmac, KeyInit, Mac};
use jsonwebtoken::crypto::{CryptoProvider, JwtSigner, JwtVerifier, KeyUtils};
use jsonwebtoken::errors::{ErrorKind, Result, new_error};
use jsonwebtoken::jwk::{EllipticCurve, ThumbprintHash};
use jsonwebtoken::signature::{Error as SigError, Signer, Verifier};
use jsonwebtoken::{Algorithm, AlgorithmFamily, DecodingKey, DecodingKeyKind, EncodingKey};
use p256::ecdsa::signature::{Signer as _, Verifier as _};
use rsa::pkcs1::{DecodeRsaPrivateKey, DecodeRsaPublicKey};
use rsa::traits::PublicKeyParts;
use rsa::{BigUint, Pkcs1v15Sign, Pss, RsaPrivateKey, RsaPublicKey};
use sha2::Digest as _;

/// Provider backed by RustCrypto crates free of RUSTSEC-2026-0387.
pub static PROVIDER: CryptoProvider = CryptoProvider {
    signer_factory: new_signer,
    verifier_factory: new_verifier,
    key_utils: KeyUtils {
        rsa_pub_components_from_private_key: rsa_components_from_private_key,
        rsa_pub_components_from_public_key: rsa_components_from_public_key,
        ec_pub_components_from_private_key: ec_components_from_private_key,
        ed_pub_components_from_private_key,
        compute_digest,
    },
};

/// Installs [`PROVIDER`] as the process default.
///
/// Must run before the first `jsonwebtoken` sign or verify call, which otherwise locks in
/// the crate-feature default. Repeated calls are no-ops.
pub fn install() {
    let _ = PROVIDER.install_default();
}

fn require_family(actual: AlgorithmFamily, expected: AlgorithmFamily) -> Result<()> {
    if actual == expected {
        Ok(())
    } else {
        Err(new_error(ErrorKind::InvalidKeyFormat))
    }
}

struct Signed<F> {
    alg: Algorithm,
    sign: F,
}

impl<F: Fn(&[u8]) -> std::result::Result<Vec<u8>, SigError>> Signer<Vec<u8>> for Signed<F> {
    fn try_sign(&self, msg: &[u8]) -> std::result::Result<Vec<u8>, SigError> {
        (self.sign)(msg)
    }
}

impl<F: Fn(&[u8]) -> std::result::Result<Vec<u8>, SigError>> JwtSigner for Signed<F> {
    fn algorithm(&self) -> Algorithm {
        self.alg
    }
}

struct Verified<F> {
    alg: Algorithm,
    verify: F,
}

impl<F: Fn(&[u8], &[u8]) -> std::result::Result<(), SigError>> Verifier<Vec<u8>>
    for Verified<F>
{
    fn verify(&self, msg: &[u8], signature: &Vec<u8>) -> std::result::Result<(), SigError> {
        (self.verify)(msg, signature)
    }
}

impl<F: Fn(&[u8], &[u8]) -> std::result::Result<(), SigError>> JwtVerifier for Verified<F> {
    fn algorithm(&self) -> Algorithm {
        self.alg
    }
}

fn signer<F>(alg: Algorithm, sign: F) -> Box<dyn JwtSigner>
where
    F: Fn(&[u8]) -> std::result::Result<Vec<u8>, SigError> + 'static,
{
    Box::new(Signed { alg, sign })
}

fn verifier<F>(alg: Algorithm, verify: F) -> Box<dyn JwtVerifier>
where
    F: Fn(&[u8], &[u8]) -> std::result::Result<(), SigError> + 'static,
{
    Box::new(Verified { alg, verify })
}

fn sig_err<E>(_: E) -> SigError {
    SigError::new()
}

macro_rules! hmac_pair {
    ($alg:expr, $hash:ty, $key:expr, sign) => {{
        let mac = Hmac::<$hash>::new_from_slice($key).map_err(|_| ErrorKind::InvalidKeyFormat)?;
        signer($alg, move |msg| {
            let mut mac = mac.clone();
            mac.update(msg);
            Ok(mac.finalize().into_bytes().to_vec())
        })
    }};
    ($alg:expr, $hash:ty, $key:expr, verify) => {{
        let mac = Hmac::<$hash>::new_from_slice($key).map_err(|_| ErrorKind::InvalidKeyFormat)?;
        verifier($alg, move |msg, signature| {
            let mut mac = mac.clone();
            mac.update(msg);
            mac.verify_slice(signature).map_err(sig_err)
        })
    }};
}

macro_rules! ecdsa_pair {
    ($alg:expr, $curve:ident, $key:expr, sign) => {{
        let key = $curve::ecdsa::SigningKey::from_pkcs8_der($key)
            .map_err(|_| ErrorKind::InvalidEcdsaKey)?;
        signer($alg, move |msg| {
            let signature: $curve::ecdsa::Signature = key.try_sign(msg).map_err(sig_err)?;
            Ok(signature.to_bytes().to_vec())
        })
    }};
    ($alg:expr, $curve:ident, $key:expr, verify) => {{
        let key = $curve::ecdsa::VerifyingKey::from_sec1_bytes($key)
            .map_err(|_| ErrorKind::InvalidEcdsaKey)?;
        verifier($alg, move |msg, signature| {
            let signature = $curve::ecdsa::Signature::from_slice(signature).map_err(sig_err)?;
            key.verify(msg, &signature).map_err(sig_err)
        })
    }};
}

fn rsa_sign<H>(alg: Algorithm, key: &EncodingKey, pss: bool) -> Result<Box<dyn JwtSigner>>
where
    H: sha2_010::Digest + sha2_010::digest::DynDigest + rsa::pkcs8::AssociatedOid,
    H: Send + Sync + 'static,
{
    let private_key = RsaPrivateKey::from_pkcs1_der(key.as_bytes())
        .map_err(|e| ErrorKind::InvalidRsaKey(e.to_string()))?;
    Ok(signer(alg, move |msg| {
        let digest = H::digest(msg);
        let mut rng = rand_core_06::OsRng;
        if pss {
            private_key.sign_with_rng(&mut rng, Pss::new::<H>(), &digest)
        } else {
            private_key.sign_with_rng(&mut rng, Pkcs1v15Sign::new::<H>(), &digest)
        }
        .map_err(sig_err)
    }))
}

fn rsa_verify<H>(alg: Algorithm, key: &DecodingKey, pss: bool) -> Result<Box<dyn JwtVerifier>>
where
    H: sha2_010::Digest + sha2_010::digest::DynDigest + rsa::pkcs8::AssociatedOid,
    H: Send + Sync + 'static,
{
    let public_key = match key.kind() {
        DecodingKeyKind::SecretOrDer(bytes) => RsaPublicKey::from_pkcs1_der(bytes)
            .map_err(|e| ErrorKind::InvalidRsaKey(e.to_string()))?,
        DecodingKeyKind::RsaModulusExponent { n, e } => {
            RsaPublicKey::new(BigUint::from_bytes_be(n), BigUint::from_bytes_be(e))
                .map_err(|e| ErrorKind::InvalidRsaKey(e.to_string()))?
        },
    };
    Ok(verifier(alg, move |msg, signature| {
        let digest = H::digest(msg);
        if pss {
            public_key.verify(Pss::new::<H>(), &digest, signature)
        } else {
            public_key.verify(Pkcs1v15Sign::new::<H>(), &digest, signature)
        }
        .map_err(sig_err)
    }))
}

fn new_signer(alg: &Algorithm, key: &EncodingKey) -> Result<Box<dyn JwtSigner>> {
    require_family(key.family(), alg.family())?;
    let bytes = key.as_bytes();
    Ok(match alg {
        Algorithm::HS256 => hmac_pair!(*alg, sha2::Sha256, bytes, sign),
        Algorithm::HS384 => hmac_pair!(*alg, sha2::Sha384, bytes, sign),
        Algorithm::HS512 => hmac_pair!(*alg, sha2::Sha512, bytes, sign),
        Algorithm::ES256 => ecdsa_pair!(*alg, p256, bytes, sign),
        Algorithm::ES384 => ecdsa_pair!(*alg, p384, bytes, sign),
        Algorithm::RS256 => rsa_sign::<sha2_010::Sha256>(*alg, key, false)?,
        Algorithm::RS384 => rsa_sign::<sha2_010::Sha384>(*alg, key, false)?,
        Algorithm::RS512 => rsa_sign::<sha2_010::Sha512>(*alg, key, false)?,
        Algorithm::PS256 => rsa_sign::<sha2_010::Sha256>(*alg, key, true)?,
        Algorithm::PS384 => rsa_sign::<sha2_010::Sha384>(*alg, key, true)?,
        Algorithm::PS512 => rsa_sign::<sha2_010::Sha512>(*alg, key, true)?,
        Algorithm::EdDSA => {
            let key = ed25519_dalek::SigningKey::from_pkcs8_der(bytes)
                .map_err(|_| ErrorKind::InvalidEddsaKey)?;
            signer(*alg, move |msg| {
                Ok(ed25519_dalek::Signer::sign(&key, msg).to_bytes().to_vec())
            })
        },
        _ => return Err(ErrorKind::InvalidAlgorithm.into()),
    })
}

fn new_verifier(alg: &Algorithm, key: &DecodingKey) -> Result<Box<dyn JwtVerifier>> {
    require_family(key.family(), alg.family())?;
    Ok(match alg {
        Algorithm::HS256 => hmac_pair!(*alg, sha2::Sha256, key.try_get_as_bytes()?, verify),
        Algorithm::HS384 => hmac_pair!(*alg, sha2::Sha384, key.try_get_as_bytes()?, verify),
        Algorithm::HS512 => hmac_pair!(*alg, sha2::Sha512, key.try_get_as_bytes()?, verify),
        Algorithm::ES256 => ecdsa_pair!(*alg, p256, key.try_get_as_bytes()?, verify),
        Algorithm::ES384 => ecdsa_pair!(*alg, p384, key.try_get_as_bytes()?, verify),
        Algorithm::RS256 => rsa_verify::<sha2_010::Sha256>(*alg, key, false)?,
        Algorithm::RS384 => rsa_verify::<sha2_010::Sha384>(*alg, key, false)?,
        Algorithm::RS512 => rsa_verify::<sha2_010::Sha512>(*alg, key, false)?,
        Algorithm::PS256 => rsa_verify::<sha2_010::Sha256>(*alg, key, true)?,
        Algorithm::PS384 => rsa_verify::<sha2_010::Sha384>(*alg, key, true)?,
        Algorithm::PS512 => rsa_verify::<sha2_010::Sha512>(*alg, key, true)?,
        Algorithm::EdDSA => {
            let bytes: [u8; 32] = key
                .try_get_as_bytes()?
                .get(..32)
                .and_then(|b| b.try_into().ok())
                .ok_or(ErrorKind::InvalidEddsaKey)?;
            let key = ed25519_dalek::VerifyingKey::from_bytes(&bytes)
                .map_err(|_| ErrorKind::InvalidEddsaKey)?;
            verifier(*alg, move |msg, signature| {
                let signature = ed25519_dalek::Signature::from_slice(signature).map_err(sig_err)?;
                key.verify_strict(msg, &signature).map_err(sig_err)
            })
        },
        _ => return Err(ErrorKind::InvalidAlgorithm.into()),
    })
}

fn rsa_components_from_private_key(der: &[u8]) -> Result<(Vec<u8>, Vec<u8>)> {
    let key = RsaPrivateKey::from_pkcs1_der(der)
        .map_err(|e| ErrorKind::InvalidRsaKey(e.to_string()))?;
    Ok((key.n().to_bytes_be(), key.e().to_bytes_be()))
}

fn rsa_components_from_public_key(der: &[u8]) -> Result<(Vec<u8>, Vec<u8>)> {
    let key = RsaPublicKey::from_pkcs1_der(der)
        .map_err(|e| ErrorKind::InvalidRsaKey(e.to_string()))?;
    Ok((key.n().to_bytes_be(), key.e().to_bytes_be()))
}

fn ec_components_from_private_key(
    der: &[u8],
    alg: Algorithm,
) -> Result<(EllipticCurve, Vec<u8>, Vec<u8>)> {
    let (curve, point) = match alg {
        Algorithm::ES256 => (
            EllipticCurve::P256,
            p256::ecdsa::SigningKey::from_pkcs8_der(der)
                .map_err(|_| ErrorKind::InvalidEcdsaKey)?
                .verifying_key()
                .to_sec1_point(false)
                .as_bytes()
                .to_vec(),
        ),
        Algorithm::ES384 => (
            EllipticCurve::P384,
            p384::ecdsa::SigningKey::from_pkcs8_der(der)
                .map_err(|_| ErrorKind::InvalidEcdsaKey)?
                .verifying_key()
                .to_sec1_point(false)
                .as_bytes()
                .to_vec(),
        ),
        _ => return Err(ErrorKind::InvalidEcdsaKey.into()),
    };
    let (x, y) = point
        .get(1..)
        .map(|xy| xy.split_at(xy.len() / 2))
        .ok_or(ErrorKind::InvalidEcdsaKey)?;
    Ok((curve, x.to_vec(), y.to_vec()))
}

fn ed_pub_components_from_private_key(der: &[u8], curve: &EllipticCurve) -> Result<Vec<u8>> {
    match curve {
        EllipticCurve::Ed25519 => Ok(ed25519_dalek::SigningKey::from_pkcs8_der(der)
            .map_err(|_| ErrorKind::InvalidEddsaKey)?
            .verifying_key()
            .as_bytes()
            .to_vec()),
        _ => Err(ErrorKind::InvalidAlgorithm.into()),
    }
}

fn compute_digest(data: &[u8], hash: ThumbprintHash) -> Result<Vec<u8>> {
    Ok(match hash {
        ThumbprintHash::SHA256 => sha2::Sha256::digest(data).to_vec(),
        ThumbprintHash::SHA384 => sha2::Sha384::digest(data).to_vec(),
        ThumbprintHash::SHA512 => sha2::Sha512::digest(data).to_vec(),
        _ => return Err(ErrorKind::InvalidAlgorithm.into()),
    })
}
