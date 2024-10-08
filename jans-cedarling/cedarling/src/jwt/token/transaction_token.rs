/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use serde::Deserialize;

/// Represents the claims contained within a Transaction Token.
///
/// A `TransactionToken` is used to track and verify specific transactions. It typically
/// contains claims that describe the transaction's details, such as the transaction ID,
/// amount, and related metadata. This token ensures that a transaction is authorized
/// and can be verified by the recipient.
///
/// Currently, the struct is a placeholder, and its implementation is pending.
///
/// # TODO
/// - Define the fields for the transaction token.
#[derive(Deserialize)]
pub struct TransactionToken;
