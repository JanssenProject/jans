/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */
use crate::bootstrap_config::BootstrapConfig;
use crate::jwt::{string_to_alg, Algorithm, ParseAlgorithmError};

pub(crate) fn parse_jwt_algorithms(
    config: &BootstrapConfig,
) -> Result<Vec<Algorithm>, ParseAlgorithmError> {
    match &config.jwt_config {
        crate::JwtConfig::Disabled => Ok(Vec::new()),
        crate::JwtConfig::Enabled {
            signature_algorithms,
        } => {
            let algorithms = signature_algorithms
                .iter()
                .map(|alg| string_to_alg(alg))
                .collect::<Result<Vec<_>, _>>()?;

            Ok(algorithms)
        },
    }
}
