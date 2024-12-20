#![cfg(all(
    target_arch = "wasm32",
    target_vendor = "unknown",
    target_os = "unknown"
))]

use cedarling::{
    AuthorizeError, AuthorizeResult, BootstrapConfig, BootstrapConfigRaw, InitCedarlingError,
    Request,
};
use serde_wasm_bindgen::Error;
use wasm_bindgen::prelude::*;

/// The instance of the Cedarling application.
#[wasm_bindgen]
#[derive(Clone)]
pub struct Cedarling {
    instance: cedarling::Cedarling,
}

#[wasm_bindgen]
pub async fn init(config: JsValue) -> Result<Cedarling, Error> {
    console_error_panic_hook::set_once();
    Cedarling::new(config).await
}

#[wasm_bindgen]
impl Cedarling {
    /// Create a new instance of the Cedarling application.
    pub async fn new(config: JsValue) -> Result<Cedarling, Error> {
        let config: BootstrapConfigRaw = serde_wasm_bindgen::from_value(config)?;

        let config = BootstrapConfig::from_raw_config(&config).map_err(|err| Error::new(err))?;

        cedarling::Cedarling::new(&config)
            .await
            .map(|instance| Cedarling { instance })
            .map_err(|err| Error::new(err))
    }

    /// Authorize request
    /// makes authorization decision based on the [`Request`]
    //
    // TODO: return typed value
    pub async fn authorize(&self, request: JsValue) -> Result<JsValue, Error> {
        let request: Request = serde_wasm_bindgen::from_value(request)?;

        let result = self
            .instance
            .authorize(request)
            .await
            .map_err(|err| Error::new(err))?;

        serde_wasm_bindgen::to_value(&result)
    }
}
