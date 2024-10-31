/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

mod can_decode_claims_with_validation;
mod can_decode_claims_without_validation;
mod can_update_local_jwks;
mod errors_on_invalid_aud;
mod errors_on_unsupported_alg;
mod utils;

use utils::*;
