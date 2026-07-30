use anyhow::Result;
use cedarling::Cedarling;

pub async fn run(config: cedarling::BootstrapConfig) -> Result<i32> {
    match Cedarling::new(&config).await {
        Ok(cedarling) => {
            let num_policies = cedarling.all_policy_metadata().len();
            println!("policy store loaded OK ({} policies)", num_policies);
            Ok(0)
        }
        Err(e) => {
            eprintln!("Error loading policy store: {:?}", e);
            Ok(2)
        }
    }
}
