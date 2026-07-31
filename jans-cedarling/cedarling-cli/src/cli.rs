use clap::{Parser, Subcommand};
use std::path::PathBuf;

#[derive(Parser)]
#[command(name = "cedarling_cli", version)]
pub struct Cli {
    #[command(flatten)]
    pub common: CommonArgs,
    
    #[command(subcommand)]
    pub cmd: Command,
}

#[derive(clap::Args)]
pub struct CommonArgs {
    #[arg(long, env = "CEDARLING_CONFIG")]
    pub config: Option<PathBuf>,
    
    #[arg(long)]
    pub policy_store: Option<PathBuf>,
    
    #[arg(long, env = "CEDARLING_LOG_TYPE")]
    pub log_type: Option<String>,
    
    #[arg(long)]
    pub log_level: Option<String>,
    
    #[arg(long)]
    pub application_name: Option<String>,
    
    #[arg(long)]
    pub no_color: bool,
}

#[derive(Subcommand)]
pub enum Command {
    Test {
        #[arg(long)]
        test_file: PathBuf,
    },
    Authorize {
        #[arg(long, requires = "principal_id")]
        principal_type: Option<String>,
        #[arg(long, requires = "principal_type")]
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
    Validate,
}
