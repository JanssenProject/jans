# JWT Service

The JwtService module is responsible for decoding and validating JWTs. This service is internal and designed to be used within the library by other modules, not directly by users of the library.

## Functionality

The primary purpose of this module is to:

- Validate and extract the claims of the `access_token` JWT.
- Validate and extract the claims of the `id_token` JWT.
- Validate and extract the claims of the `userinfo_token` JWT.

These tokens are validated sequentially, ensuring that the `access_token` proves client authentication (authN), while the id_token and `userinfo_token` are correlated to the same authenticated entity.

## Strict Mode

If the bootstrap property `CEDARLING_ID_TOKEN_TRUST_MODE` is set to `STRICT`, the following validation checks are implemented:

1. Validate that the `id_token`'s `sub` claim is the same as the `access_token`'s `client_id` claim.
2. Validate that the `userinfo_token`'s `sub` claim is the same as the `id_token`'s `sub` claim.
3. Validate that the `userinfo_token`'s `aud` claim is the same as the `access_tokens`'s `client_id` claim.

## Initialization

The JwtService can be initialized via the `new` function which requires a `JwtConfig` and `TrustedIssuer`s.

```rs
#[derive(Debug, PartialEq)]
pub struct JwtConfig {
    pub jwks: Option<String>,
    pub jwt_sig_validation: bool,
    pub jwt_status_validation: bool,
    pub id_token_trust_mode: IdTokenTrustMode,
    pub signature_algorithms_supported: HashSet<Algorithm>,
    pub access_token_config: TokenValidationConfig,
    pub id_token_config: TokenValidationConfig,
    pub userinfo_token_config: TokenValidationConfig,
}

impl JwtService{
    pub fn new(
        config: &JwtConfig,
        trusted_issuers: Option<HashMap<String, TrustedIssuer>>,
    ) -> Result<Self, JwtServiceInitError> {
}
```

## Decoding and Validating Tokens

The JwtService exposes a `process_tokens` function that decodes and validates both the access_token and id_token. It expects JSON Web Tokens (JWTs) strings as input.

```rs
pub fn process_tokens<'a, A, I, U>(
    &'a self,
    access_token: &'a str,
    id_token: &'a str,
    userinfo_token: Option<&'a str>,
) -> Result<ProcessTokensResult<A, I, U>, JwtProcessingError>
where
    A: DeserializeOwned,
    I: DeserializeOwned,
    U: DeserializeOwned,
```

## Example

Below is an example on how to initialize and use the JwtService.

```rs
let jwt_service = JwtService::new(
    &JwtConfig {
        jwks: Some(local_jwks),
        jwt_sig_validation: true,
        jwt_status_validation: false,
        id_token_trust_mode: IdTokenTrustMode::Strict,
        signature_algorithms_supported: HashSet::from_iter([Algorithm::HS256]),
        access_token_config: TokenValidationConfig::access_token(),
        id_token_config: TokenValidationConfig::id_token(),
        userinfo_token_config: TokenValidationConfig::userinfo_token(),
    },
    None,
)
.expect("Should create JwtService");

jwt_service
    .process_tokens::<Value, Value, Value>(&access_tkn, &id_tkn, Some(&userinfo_tkn))
    .expect("Should process JWTs");
```


