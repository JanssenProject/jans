#![allow(clippy::missing_errors_doc)]
use anyhow::{Context, Result};
use cedarling::Cedarling;

pub async fn run(config: cedarling::BootstrapConfig) -> Result<i32> {
    let cedarling = Cedarling::new(&config).await.context("failed to initialize Cedarling")?;
    let num_policies = cedarling.all_policy_metadata().len();
    println!("policy store loaded OK ({num_policies} policies)");
    Ok(0)
}
