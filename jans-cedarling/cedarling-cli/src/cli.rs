// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use clap::{Parser, Subcommand};
use std::path::PathBuf;

/// Command-line interface for the Cedarling application.
#[derive(Parser)]
#[command(name = "cedarling_cli", version)]
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

    /// Logger type (e.g., std_out, memory, off)
    #[arg(long, env = "CEDARLING_LOG_TYPE")]
    pub log_type: Option<String>,

    /// Log level (e.g., info, debug, warn, error)
    #[arg(long)]
    pub log_level: Option<String>,

    /// Application name identifier
    #[arg(long)]
    pub application_name: Option<String>,

    /// Disable colored output
    #[arg(long)]
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
    Authorize {
        /// The entity type of the principal
        #[arg(long, requires = "principal_id")]
        principal_type: Option<String>,
        /// The unique ID of the principal
        #[arg(long, requires = "principal_type")]
        principal_id: Option<String>,
        /// Optional JSON string containing principal attributes
        #[arg(long)]
        principal_attrs: Option<String>, // JSON
        /// The action to evaluate
        #[arg(long)]
        action: String,
        /// The entity type of the resource
        #[arg(long)]
        resource_type: String,
        /// The unique ID of the resource
        #[arg(long)]
        resource_id: String,
        /// Optional JSON string containing resource attributes
        #[arg(long)]
        resource_attrs: Option<String>, // JSON
        /// Optional JSON string containing contextual data
        #[arg(long, default_value = "{}")]
        context: String, // JSON
    },
    /// Validate a policy store against schema and semantic rules
    Validate,
}
