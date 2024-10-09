/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

#[allow(missing_docs)]
mod access_token;
#[allow(missing_docs)]
mod id_token;
#[allow(missing_docs)]
mod transaction_token;
#[allow(missing_docs)]
mod userinfo_token;

pub use access_token::*;
pub use id_token::*;
pub use transaction_token::*;
pub use userinfo_token::*;

/// Represents expected claims
#[allow(missing_docs)]
pub enum Claims {
    AccessToken(AccessToken),
    IdToken(IdToken),
    TransactionToken(TransactionToken),
    UserInfoToken(UserinfoToken),
}
