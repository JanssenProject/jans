use clap::{Parser, Subcommand};
use std::path::PathBuf;

#[derive(Parser)]
#[command(name = "cedarling_cli", version)]
struct Cli {
    #[command(flatten)]
    common: CommonArgs,
    
    #[command(subcommand)]
    cmd: Command,
}

#[derive(clap::Args)]
struct CommonArgs {
    #[arg(long, env = "CEDARLING_CONFIG")]
    config: Option<PathBuf>,
    
    #[arg(long, env = "CEDARLING_POLICY_STORE_URI")]
    policy_store: Option<PathBuf>,
    
    #[arg(long, env = "CEDARLING_LOG_TYPE")]
    log_type: Option<String>,
    
    #[arg(long)]
    log_level: Option<String>,
    
    #[arg(long)]
    application_name: Option<String>,
    
    #[arg(long)]
    no_color: bool,
}

#[derive(Subcommand)]
enum Command {
    Test {
        #[arg(long)]
        test_file: PathBuf,
    },
    Authorize {
        #[arg(long)]
        principal_type: Option<String>,
        #[arg(long)]
        principal_id: Option<String>,
        #[arg(long)]
        principal_attrs: Option<String>, // JSON
        #[arg(long)]
        action: String,
        #[arg(long)]
        resource_type: String,
        #[arg(long)]
        resource_id: String,
        #[arg(long)]
        resource_attrs: Option<String>,  // JSON
        #[arg(long, default_value = "{}")]
        context: String, // JSON
    },
    Validate {},
}

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    let _cli = Cli::parse();
    
    Ok(())
}
