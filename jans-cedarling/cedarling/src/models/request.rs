/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

/// Box to store authorization data
#[derive(Debug, serde::Deserialize)]
pub struct Request<'a> {
    /// Access token raw value
    pub access_token: &'a str,
}
