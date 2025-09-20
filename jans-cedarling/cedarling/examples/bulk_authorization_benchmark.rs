// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedarling::*;
use serde_json::json;
use std::collections::{HashMap, HashSet};
use std::time::Instant;

static POLICY_STORE_RAW: &str = include_str!("../../test_files/policy-store_ok.yaml");

use stats_alloc::{INSTRUMENTED_SYSTEM, Region, StatsAlloc};
use std::alloc::System;

#[global_allocator]
static GLOBAL: &StatsAlloc<System> = &INSTRUMENTED_SYSTEM;

#[tokio::main(flavor = "current_thread")]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let reg = Region::new(&GLOBAL);

    println!("=== Cedarling Bulk Authorization Benchmark ===\n");
    println!("Simulating OpenSearch integration with 2000 documents per query\n");

    // Initialize Cedarling
    let cedarling = Cedarling::new(&BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::Off,
            log_level: LogLevel::INFO,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(POLICY_STORE_RAW.to_string()),
        },
        jwt_config: JwtConfig {
            jwks: None,
            jwt_sig_validation: false,
            jwt_status_validation: false,
            signature_algorithms_supported: HashSet::new(),
        }
        .allow_all_algorithms(),
        authorization_config: AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            id_token_trust_mode: IdTokenTrustMode::Never,
            ..Default::default()
        },
        entity_builder_config: EntityBuilderConfig::default().with_user().with_workload(),
        lock_config: None,
        max_base64_size: None,
        max_default_entities: None,
        token_cache_max_ttl_secs: 60,
    })
    .await?;

    // Generate 2000 test documents (similar to the OpenSearch benchmark)
    let documents = generate_test_documents(2000);
    println!("Generated {} test documents", documents.len());

    // Test 1: Single authorization (baseline)
    println!("\n1. Single Authorization (Baseline):");
    let single_doc = &documents[0];
    let mut total_time = 0;
    let iterations = 100;

    for i in 0..iterations {
        let start = Instant::now();
        let _result = cedarling.authorize_unsigned(single_doc.clone()).await?;
        let duration = start.elapsed();
        total_time += duration.as_micros();

        if i % 20 == 0 {
            println!("  Iteration {}: {} μs", i, duration.as_micros());
        }
    }

    let avg_single = total_time / iterations;
    println!(
        "  Average single authorization: {} μs ({:.3} ms)",
        avg_single,
        avg_single as f64 / 1000.0
    );

    // Test 2: Batch authorization (2000 documents)
    println!("\n2. Batch Authorization (2000 documents):");
    let batch_start = Instant::now();
    let mut successful_auths = 0;
    let mut failed_auths = 0;

    for (i, doc) in documents.iter().enumerate() {
        let auth_start = Instant::now();
        match cedarling.authorize_unsigned(doc.clone()).await {
            Ok(result) => {
                if result.decision {
                    successful_auths += 1;
                } else {
                    failed_auths += 1;
                }
            },
            Err(_) => {
                failed_auths += 1;
            },
        }
        let auth_duration = auth_start.elapsed();

        if i % 200 == 0 {
            println!(
                "  Processed {} documents: {} μs per auth",
                i,
                auth_duration.as_micros()
            );
        }
    }

    let batch_duration = batch_start.elapsed();
    let total_docs = documents.len();
    let avg_per_doc = batch_duration.as_micros() / total_docs as u128;

    println!("\n  Batch Results:");
    println!("    Total time: {:.2} ms", batch_duration.as_millis());
    println!("    Documents processed: {}", total_docs);
    println!(
        "    Average per document: {} μs ({:.3} ms)",
        avg_per_doc,
        avg_per_doc as f64 / 1000.0
    );
    println!("    Successful authorizations: {}", successful_auths);
    println!("    Failed authorizations: {}", failed_auths);

    // Test 3: Simulate OpenSearch-like processing with different batch sizes
    println!("\n3. Simulating Different Batch Sizes:");
    let batch_sizes = vec![10, 50, 100, 500, 1000, 2000];

    for batch_size in batch_sizes {
        let batch_docs: Vec<_> = documents.iter().take(batch_size).collect();
        let batch_start = Instant::now();

        for doc in &batch_docs {
            let _ = cedarling.authorize_unsigned((*doc).clone()).await;
        }

        let batch_duration = batch_start.elapsed();
        let avg_per_doc = batch_duration.as_micros() / batch_size as u128;

        println!(
            "  Batch size {}: {:.2} ms total, {} μs per doc ({:.3} ms)",
            batch_size,
            batch_duration.as_millis(),
            avg_per_doc,
            avg_per_doc as f64 / 1000.0
        );
    }

    // Test 4: Memory usage simulation
    println!("\n4. Memory Usage Analysis:");
    let mem_before = get_memory_usage(&reg);

    // Process a large batch to see memory impact
    let large_batch: Vec<_> = documents.iter().take(1000).collect();
    let _results: Vec<_> = futures::future::join_all(
        large_batch
            .iter()
            .map(|doc| cedarling.authorize_unsigned((*doc).clone())),
    )
    .await
    .into_iter()
    .collect::<Result<Vec<_>, _>>()?;

    let mem_after = get_memory_usage(&reg);
    println!("  Memory before: {} MB", mem_before);
    println!("  Memory after: {} MB", mem_after);
    println!("  Memory increase: {} MB", mem_after - mem_before);

    // Test 5: Comparison with OpenSearch benchmark expectations
    println!("\n5. Comparison with OpenSearch Benchmark:");
    println!("  OpenSearch benchmark: 2.0ms per document");
    println!(
        "  Our batch result: {:.3}ms per document",
        avg_per_doc as f64 / 1000.0
    );
    println!("  Ratio: {:.2}x", (avg_per_doc as f64 / 1000.0) / 2.0);

    if avg_per_doc as f64 / 1000.0 > 2.0 {
        println!(
            "  Our result is {:.2}x slower than OpenSearch benchmark",
            (avg_per_doc as f64 / 1000.0) / 2.0
        );
    } else {
        println!(
            "  Our result is {:.2}x faster than OpenSearch benchmark",
            2.0 / (avg_per_doc as f64 / 1000.0)
        );
    }

    println!("\n=== Benchmark Complete ===");
    Ok(())
}

fn generate_test_documents(count: usize) -> Vec<RequestUnsigned> {
    let mut documents = Vec::new();

    for i in 0..count {
        // Generate random data similar to the OpenSearch benchmark
        let name = format!("Student_{}", i);
        let grad_year = 2020 + (i % 7); // 2020-2026
        let gpa = (i as f64 * 0.001) % 5.0; // 0-5 GPA

        let principals = vec![EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Jans::User".to_string(),
                id: format!("user_{}", i),
            },
            attributes: HashMap::from([
                ("sub".to_string(), json!(format!("sub_{}", i))),
                (
                    "email".to_string(),
                    json!(format!("student{}@university.edu", i)),
                ),
                ("username".to_string(), json!(name.clone())),
                ("country".to_string(), json!("US")),
                ("role".to_string(), json!("Student")),
                ("gpa".to_string(), json!(gpa)),
                ("grad_year".to_string(), json!(grad_year)),
            ]),
        }];

        let document = RequestUnsigned {
            principals,
            action: "Jans::Action::\"Update\"".to_string(),
            context: serde_json::json!({}),
            resource: EntityData {
                cedar_mapping: CedarEntityMapping {
                    entity_type: "Jans::Issue".to_string(),
                    id: format!("document_{}", i),
                },
                attributes: HashMap::from_iter([
                    (
                        "org_id".to_string(),
                        serde_json::Value::String("university_123".to_string()),
                    ),
                    (
                        "country".to_string(),
                        serde_json::Value::String("US".to_string()),
                    ),
                    ("name".to_string(), serde_json::Value::String(name)),
                    (
                        "gpa".to_string(),
                        serde_json::Value::Number(serde_json::Number::from_f64(gpa).unwrap()),
                    ),
                    (
                        "grad_year".to_string(),
                        serde_json::Value::Number(serde_json::Number::from(grad_year)),
                    ),
                ]),
            },
        };

        documents.push(document);
    }

    documents
}

fn get_memory_usage<T: std::alloc::GlobalAlloc>(region: &Region<T>) -> u64 {
    (region.change().allocations - region.change().deallocations) as u64
}
