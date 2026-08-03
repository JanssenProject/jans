// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedarling_cli::cli::{Cli, Command};
use cedarling_cli::{authorize, config, test, validate};
use clap::Parser;

fn unwrap_or_exit<T, E: std::fmt::Debug>(result: Result<T, E>) -> T {
    match result {
        Ok(v) => v,
        Err(e) => {
            eprintln!("Error: {e:?}");
            std::process::exit(2);
        },
    }
}

#[tokio::main]
async fn main() {
    let cli = Cli::parse();

    if cli.common.no_color {
        colored::control::set_override(false);
    }

    let bootstrap_config = unwrap_or_exit(config::resolve_bootstrap(&cli.common));

    let code = match cli.cmd {
        Command::Test { test_file } => {
            unwrap_or_exit(test::runner::run(bootstrap_config, test_file).await)
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
        } => unwrap_or_exit(
            authorize::run(
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
            .await,
        ),
        Command::Validate => unwrap_or_exit(validate::run(bootstrap_config).await),
    };

    std::process::exit(code);
}
