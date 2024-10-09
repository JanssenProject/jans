/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

pub struct AccessToken {
    aud: String,
    iss: String,
    jti: String,
    scope: String,
}

impl AccessToken {
    pub fn new(
        aud: impl Into<String>,
        iss: impl Into<String>,
        jti: impl Into<String>,
        scope: impl Into<String>,
    ) -> Self {
        Self {
            aud: aud.into(),
            iss: iss.into(),
            jti: jti.into(),
            scope: scope.into(),
        }
    }

    pub fn aud(&self) -> &String {
        &self.aud
    }

    pub fn iss(&self) -> &String {
        &self.iss
    }

    pub fn jti(&self) -> &String {
        &self.jti
    }

    pub fn scope(&self) -> &String {
        &self.scope
    }
}
