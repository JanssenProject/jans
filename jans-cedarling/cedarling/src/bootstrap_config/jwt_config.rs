/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use jsonwebtoken::Algorithm;
use std::collections::HashSet;

/// A set of properties used to configure JWT in the `Cedarling` application.
#[derive(Debug, Clone, PartialEq)]
pub enum JwtConfig {
    /// `CEDARLING_JWT_VALIDATION` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.  
    /// Represent `Disabled` value.  
    /// Meaning no JWT validation and no controls if Cedarling will discard id_token without an access token with the corresponding client_id.
    Disabled,
    /// `CEDARLING_JWT_VALIDATION` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.  
    /// Represent `Enabled` value
    Enabled {
        /// `CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED` in [bootstrap properties](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties) documentation.
        signature_algorithms: HashSet<Algorithm>,
    },
}
