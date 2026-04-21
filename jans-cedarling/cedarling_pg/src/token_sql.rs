// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! SQL-callable helpers for the `cedarling.tokens` GUC (ORM-friendly `SET` / read).

use pgrx::datum::JsonB;
use pgrx::prelude::*;
use pgrx::spi::Result as SpiResult;

use crate::guc_config;

/// Sets `cedarling.tokens` from a `jsonb` value using `set_config(..., is_local=true)` (transaction-scoped).
///
/// `jsonb` is always valid JSON; the GUC check hook validates the text form on assignment.
///
/// **Testing note:** `PostgreSQL` SPI (used by `Spi::connect` inside this module) rejects transaction
/// control statements such as `BEGIN` / `SAVEPOINT` from nested `Spi::run` calls in `#[pg_test]`
/// bodies (`SPI_ERROR_TRANSACTION`). Verifying `ROLLBACK` / `ROLLBACK TO SAVEPOINT` against these
/// assignments is better done from a client session (e.g. `psql` or a small integration test), not
/// from inside extension SQL helpers.
#[pg_extern]
#[allow(clippy::needless_pass_by_value)] // `#[pg_extern]` maps Rust parameters from PostgreSQL call convention
pub fn cedarling_set_tokens(tokens: JsonB) -> SpiResult<()> {
    let json_str = serde_json::to_string(&tokens.0)
        .expect("serde_json::to_string on serde_json::Value should not fail");
    set_tokens_guc(&json_str, true)
}

/// Clears `cedarling.tokens` for the current transaction (`set_config` with empty value, `is_local=true`).
#[pg_extern]
pub fn cedarling_clear_tokens() -> SpiResult<()> {
    set_tokens_guc("", true)
}

/// Returns the current `cedarling.tokens` as `jsonb`, or SQL `NULL` if unset/empty.
#[pg_extern]
pub fn cedarling_current_tokens() -> Option<JsonB> {
    let s = guc_config::tokens_utf8()?;
    let trimmed = s.trim();
    if trimmed.is_empty() {
        return None;
    }
    let value: serde_json::Value = serde_json::from_str(trimmed).ok()?;
    Some(JsonB(value))
}

fn set_tokens_guc(json: &str, is_local: bool) -> SpiResult<()> {
    Spi::connect_mut(|client| {
        client.update(
            "SELECT set_config('cedarling.tokens', $1, $2)",
            None,
            &[json.into(), is_local.into()],
        )?;
        Ok(())
    })
}
