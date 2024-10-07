/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use cedarling::{
    BootstrapConfig, Cedarling, JwtConfig, LogConfig, LogStorage, LogTypeConfig, MemoryLogConfig,
    PolicyStoreConfig, PolicyStoreSource, Request, ResourceData,
};
use std::{collections::HashMap, env};

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

    let access_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJseFRtQ1ZSRlR4T2pKZ3ZFRXBvek1RIiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjIwMSwidXJpIjoiaHR0cHM6Ly9hZG1pbi11aS10ZXN0LmdsdXUub3JnL2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19._eQT-DsfE_kgdhA0YOyFxxPEMNw44iwoelWa5iU1n9s";

    let result = cedarling.authorize(Request {
        access_token,
        action: "Jans::Action::\"Update\"".to_string(),
        context: serde_json::json!({}),
        resource: ResourceData {
            id: "random_id".to_string(),
            resource_type: "Jans::Issue".to_string(),
            payload: HashMap::from_iter([(
                "org_id".to_string(),
                serde_json::Value::String("some_long_id".to_string()),
            )]),
        },
    });
    if let Err(ref e) = &result {
        eprintln!("Error while authorizing: {}\n\n", e)
    }
    let result = result?;

    println!("decision: {:?}", result.decision());
    println!("diagnostics: {:?}", result.diagnostics());

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
