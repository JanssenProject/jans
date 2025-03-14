use josekit::Value;
use josekit::jwk::Jwk;
use josekit::jwk::alg::ec::EcCurve;
use josekit::jwk::alg::ed::EdCurve;
use josekit::jws::JwsHeader;
use josekit::jws::alg::{
    ecdsa::EcdsaJwsAlgorithm::Es256, eddsa::EddsaJwsAlgorithm::Eddsa,
    hmac::HmacJwsAlgorithm::Hs256, rsassa::RsassaJwsAlgorithm::Rs256,
};
use josekit::jwt::{JwtPayload, encode_with_signer};
use std::error::Error;
use std::sync::atomic::{AtomicU32, Ordering};

#[derive(Eq, Hash, PartialEq, Clone, Copy)]
pub enum Algorithm {
    HS256,
    RS256,
    ES256,
    EdDSA,
}

/// A signed JWT and the Key used to sign it
#[derive(Debug)]
pub struct SignedToken {
    pub token: String,
    pub jwk: Jwk,
}

/// Generates a JWT signed with the given algorithm. Shorthand for [`generate_token`].
///
/// # Usage
///
/// ```
/// use test_utils::Algorithm;
///
/// let jwt = jwt!(
///     Algorithm::RS256,
///     {
///         "iss": "https://example.test.com/",
///         "sub": "bob",
///         "jti": "abc123",
///     }
/// ).unwrap();
/// ```
#[macro_export]
macro_rules! jwt {
    ($alg:expr, $($payload:tt)*) => {{
        let payload = serde_json::json!($($payload)*);
        $crate::generate_token::generate_token(payload, $alg)
    }};
}

/// Creates a JWT from a [`serde_json::Value`].
pub fn generate_token(payload: Value, algorithm: Algorithm) -> Result<SignedToken, Box<dyn Error>> {
    static KEY_ID_COUNTER: AtomicU32 = AtomicU32::new(0);

    let Value::Object(payload) = payload else {
        return Err("value must be an object to be a JWT payload".into());
    };
    let payload = JwtPayload::from_map(payload)?;

    let jwk = match algorithm {
        Algorithm::HS256 => Jwk::generate_oct_key(255),
        Algorithm::RS256 => Jwk::generate_rsa_key(2048),
        Algorithm::ES256 => Jwk::generate_ec_key(EcCurve::P256),
        Algorithm::EdDSA => Jwk::generate_ed_key(EdCurve::Ed25519),
    }?;
    let kid_num = KEY_ID_COUNTER.fetch_add(1, Ordering::AcqRel);
    let kid_str = format!("key_{}", kid_num);

    let mut header = JwsHeader::new();
    header.set_token_type("JWT");
    header.set_key_id(kid_str.clone());

    let token = match algorithm {
        Algorithm::HS256 => sign_token_hs256(&payload, &header, &jwk),
        Algorithm::RS256 => sign_token_rs256(&payload, &header, &jwk),
        Algorithm::ES256 => sign_token_es256(&payload, &header, &jwk),
        Algorithm::EdDSA => sign_token_eddsa(&payload, &header, &jwk),
    }?;

    let mut jwk = match algorithm {
        Algorithm::HS256 => jwk,
        _ => jwk.to_public_key()?,
    };
    jwk.set_key_id(kid_str);
    Ok(SignedToken { token, jwk })
}

fn sign_token_hs256(
    payload: &JwtPayload,
    header: &JwsHeader,
    jwk: &Jwk,
) -> Result<String, Box<dyn Error>> {
    let signer = Hs256.signer_from_jwk(&jwk)?;
    let token = encode_with_signer(&payload, &header, &signer)?;
    Ok(token)
}

fn sign_token_rs256(
    payload: &JwtPayload,
    header: &JwsHeader,
    jwk: &Jwk,
) -> Result<String, Box<dyn Error>> {
    let signer = Rs256.signer_from_jwk(&jwk)?;
    let token = encode_with_signer(&payload, &header, &signer)?;
    Ok(token)
}

fn sign_token_es256(
    payload: &JwtPayload,
    header: &JwsHeader,
    jwk: &Jwk,
) -> Result<String, Box<dyn Error>> {
    let signer = Es256.signer_from_jwk(&jwk)?;
    let token = encode_with_signer(&payload, &header, &signer)?;
    Ok(token)
}

fn sign_token_eddsa(
    payload: &JwtPayload,
    header: &JwsHeader,
    jwk: &Jwk,
) -> Result<String, Box<dyn Error>> {
    let signer = Eddsa.signer_from_jwk(&jwk)?;
    let token = encode_with_signer(&payload, &header, &signer)?;
    Ok(token)
}
