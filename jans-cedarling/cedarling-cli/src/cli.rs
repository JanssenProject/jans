// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use clap::{Parser, Subcommand};
use std::path::PathBuf;

/// Command-line interface for the Cedarling application.
#[derive(Parser)]
#[command(version)]
pub struct Cli {
    /// Common arguments applicable to all commands
    #[command(flatten)]
    pub common: CommonArgs,

    /// The subcommand to execute
    #[command(subcommand)]
    pub cmd: Command,
}

/// Arguments common to all Cedarling CLI subcommands.
#[derive(clap::Args)]
pub struct CommonArgs {
    /// Path to a JSON/YAML bootstrap configuration file
    #[arg(long, env = "CEDARLING_CONFIG")]
    pub config: Option<PathBuf>,

    /// Path or URL to the policy store (overrides config)
    #[arg(long)]
    pub policy_store: Option<PathBuf>,

    /// URL to a remote .cjar policy store (overrides config)
    #[arg(long)]
    pub policy_store_cjar_url: Option<String>,

    /// Logger type (e.g., `std_out`, `memory`, `off`)
    #[arg(long, env = "CEDARLING_LOG_TYPE")]
    pub log_type: Option<cedarling::LoggerType>,

    /// Log level (e.g., fatal, error, warn, info, debug, trace)
    #[arg(long, env = "CEDARLING_LOG_LEVEL")]
    pub log_level: Option<cedarling::LogLevel>,

    /// Application name identifier
    #[arg(long)]
    pub application_name: Option<String>,

    /// Disable colored output
    #[arg(long, env = "NO_COLOR")]
    pub no_color: bool,
}

/// Available subcommands for the Cedarling CLI.
#[derive(Subcommand)]
pub enum Command {
    /// Execute a suite of tests defined in a YAML specification
    Test {
        /// Path to the YAML file containing the test specification
        #[arg(long)]
        test_file: PathBuf,
    },
    /// Evaluate a single authorization request
    Authorize(#[command(flatten)] crate::authorize::AuthorizeArgs),
    /// Validate a policy store against schema and semantic rules
    Validate {
        /// Treat skipped validations as errors
        #[arg(long)]
        strict: bool,
    },
}
