/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use cedarling::{
    BootstrapConfig, Cedarling, JwtConfig, LogConfig, LogStorage, LogTypeConfig, MemoryLogConfig,
    PolicyStoreConfig, PolicyStoreSource,
};
use std::env;

static POLICY_STORE_RAW: &str = include_str!("../src/init/test_files/policy-store_ok.json");

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Collect command-line arguments
    let args: Vec<String> = env::args().collect();

    // Ensure at least one argument is provided (the program name itself is the first argument)
    if args.len() < 2 {
        eprintln!("Usage: {} <log_type> [ttl in seconds]", args[0]);
        eprintln!("<log_type> can be one of off,stdout,memory");
        std::process::exit(1);
    }

    // Parse the log type from the first argument
    let log_type_arg = &args[1];
    let log_type = match log_type_arg.as_str() {
        "off" => LogTypeConfig::Off,
        "stdout" => LogTypeConfig::StdOut,
        "lock" => LogTypeConfig::Lock,
        "memory" => extract_memory_config(args),
        _ => {
            eprintln!("Invalid log type, defaulting to StdOut.");
            LogTypeConfig::StdOut
        },
    };

    println!("Cedarling initialized with log type: {:?}", log_type);
    let cedarling = Cedarling::new(BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig { log_type },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Json(POLICY_STORE_RAW.to_string()),
            store_id: None,
        },
        jwt_config: JwtConfig::Disabled,
    })?;

    println!("Stage 1:");
    let logs_ids = cedarling.get_log_ids();
    println!(
        "Show results of get_logs(): returns a list of all log ids: {:?}",
        &logs_ids
    );
    println!("\n\nStage 2:\nShow result of get_log_by_id for each key.");
    for id in logs_ids {
        let entry = cedarling
            .get_log_by_id(&id)
            .map(|v| serde_json::json!(v).to_string());
        println!("\nkey:{}\nvalue:{:?}", id, entry);
    }

    println!("\n\n Stage 3:\nShow result of pop_logs");
    for (i, entry) in cedarling.pop_logs().iter().enumerate() {
        println!("entry n:{i}\nvalue: {}", serde_json::json!(entry))
    }

    println!("\n\n Stage 4:\nShow len of keys left using get_log_ids");
    println!("Number of keys left: {:?}", cedarling.get_log_ids().len());

    Ok(())
}

fn extract_memory_config(args: Vec<String>) -> LogTypeConfig {
    if args.len() < 3 {
        eprintln!("Memory log type requires two additional arguments: ttl value in seconds");
        std::process::exit(1);
    }
    // Parse additional arguments for MemoryLogConfig
    let log_ttl: u64 = args[2]
        .parse()
        .expect("Invalid ttl value, should be integer");
    LogTypeConfig::Memory(MemoryLogConfig { log_ttl })
}
