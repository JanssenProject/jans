# JWT Service

## Overview

The JwtService module is responsible for decoding and validating JWTs. This service is internal and designed to be used within the library by other modules, not directly by users of the library.

## Functionality

The primary purpose of this module is to:

- Validate the `access_token` JWT (which is also the client_id).
- Validate the `id_token` JWT and ensure that `id_token.aud` matches the `client_id` from the `access_token`.

These tokens are validated sequentially, ensuring that the `access_token` proves client authentication (authN), while the id_token and `userinfo_token` are correlated to the same authenticated entity.

## Token Validation Steps:

1. Validate the `access_token` and keep track of it's `aud`. This `aud` is also the `client_id`.
2. Validate the `id_token` and ensure that its `aud` matches the `access_token`'s `client_id`.

## Initialization

The `JwtService` is initialized through dependency injection and configuration. It requires a dependency map and a configuration object to set up.

```rust
pub fn new_with_container(dep_map: &di::DependencyMap, config: JwtConfig) -> Result<Self, Error>
```

## Usage

### Decoding and Validating Tokens

The JwtService exposes a `decode_tokens` function that decodes and validates both the access_token and id_token. It expects JSON Web Tokens (JWTs) strings as input.

```rust
pub fn decode_tokens<A, T>(
    &self,
    access_token_str: &str,
    id_token_str: &str,
) -> Result<(A, T), Error>
where
    A: DeserializeOwned
    T: DeserializeOwned
```

### Example

```rust
let jwt_service = JwtService::new_with_container(&dep_map, jwt_config)?;
let (access_token_claims, id_token_claims) = jwt_service.decode_tokens::<AccessTokenClaims, IdTokenClaims>(
    access_token_str,
    id_token_str,
)?;
```


