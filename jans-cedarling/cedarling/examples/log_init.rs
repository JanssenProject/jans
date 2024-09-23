use cedarling::{
    AuthzConfig, BootstrapConfig, Cedarling, LogConfig, LogStorage, LogType, MemoryLogConfig,
};
use std::env;

fn main() {
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
        "off" => LogType::Off,
        "stdout" => LogType::StdOut,
        "lock" => LogType::Lock,
        "memory" => extract_memory_config(args),
        _ => {
            eprintln!("Invalid log type, defaulting to StdOut.");
            LogType::StdOut
        },
    };

    println!("Authz initialized with log type: {:?}", log_type);

    // Create the Authz instance with the selected log type
    let authz = Cedarling::new(BootstrapConfig {
        authz_config: AuthzConfig {
            application_name: "test_app".to_string(),
        },
        log_config: LogConfig { log_type },
    });

    println!("Stage 1:");
    let logs_ids = authz.get_log_ids();
    println!(
        "Show results of get_logs(): returns a list of all log ids: {:?}",
        &logs_ids
    );
    println!("\n\n Stage 2:\nShow result of get_log_by_id for each key.");
    for id in logs_ids {
        let entry = authz
            .get_log_by_id(&id)
            .map(|v| serde_json::json!(v).to_string());
        println!("\nkey:{}\nvalue:{:?}", id, entry);
    }

    println!("\n\n Stage 3:\nShow result of pop_logs");
    for (i, entry) in authz.pop_logs().iter().enumerate() {
        println!("entry n:{i}\nvalue: {}", serde_json::json!(entry))
    }

    println!("\n\n Stage 4:\nShow len of keys left using get_log_ids");
    println!("Number of keys left: {:?}", authz.get_log_ids().len());
}

fn extract_memory_config(args: Vec<String>) -> LogType {
    if args.len() < 3 {
        eprintln!("Memory log type requires two additional arguments: ttl value in seconds");
        std::process::exit(1);
    }
    // Parse additional arguments for MemoryLogConfig
    let log_ttl: u64 = args[2]
        .parse()
        .expect("Invalid ttl value, should be integer");
    LogType::Memory(MemoryLogConfig { log_ttl })
}
