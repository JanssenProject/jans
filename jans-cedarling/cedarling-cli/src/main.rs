use clap::Parser;
use cedarling_cli::cli::{Cli, Command};
use cedarling_cli::{authorize, config, test, validate};

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    let cli = Cli::parse();
    
    if cli.common.no_color {
        colored::control::set_override(false);
    }
    
    let bootstrap_config = match config::resolve_bootstrap(&cli.common) {
        Ok(c) => c,
        Err(e) => {
            eprintln!("Error: {e:?}");
            std::process::exit(2);
        }
    };

    match cli.cmd {
        Command::Test { test_file } => match test::runner::run(bootstrap_config, test_file).await {
            Ok(code) => std::process::exit(code),
            Err(e) => {
                eprintln!("Error: {e:?}");
                std::process::exit(2);
            }
        },
        Command::Authorize {
            principal_type,
            principal_id,
            principal_attrs,
            action,
            resource_type,
            resource_id,
            resource_attrs,
            context,
        } => {
            match authorize::run(
                bootstrap_config,
                authorize::AuthorizeArgs {
                    principal_type,
                    principal_id,
                    principal_attrs,
                    action,
                    resource_type,
                    resource_id,
                    resource_attrs,
                    context,
                },
            )
            .await
            {
                Ok(code) => std::process::exit(code),
                Err(e) => {
                    eprintln!("Error: {e:?}");
                    std::process::exit(2);
                }
            }
        }
        Command::Validate => match validate::run(bootstrap_config).await {
            Ok(code) => std::process::exit(code),
            Err(e) => {
                eprintln!("Error: {e:?}");
                std::process::exit(2);
            }
        },
    }
}
