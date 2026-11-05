// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Cost of TRACE on the authorization path and throughput of the record emitter,
//! against a mock Lock that accepts every record.

use std::hint::black_box;
use std::io::{Cursor, Write};
use std::num::NonZeroUsize;
use std::path::Path;
use std::sync::Arc;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::time::{Duration, Instant};

use cedarling::{
    AuthorizationConfig, AuthorizeOptions, BootstrapConfig, Cedarling, DataStoreConfig, EntityData,
    HttpClientConfig, JwtConfig, LockServiceConfig, LogConfig, LogLevel, LogTypeConfig,
    PolicyStoreConfig, PolicyStoreSource, RequestUnsigned, TraceConfig, TraceContext,
    TraceEndpoint, TraceSigningKey,
};
use criterion::{Criterion, Throughput, criterion_group, criterion_main};
use mockito::{Server, ServerGuard};
use serde::de::IgnoredAny;
use serde_json::json;
use tokio::runtime::Runtime;
use zip::write::{ExtendedFileOptions, FileOptions};
use zip::{CompressionMethod, ZipWriter};

const BULK_PATH: &str = "/audit/trace/bulk";

/// Decisions per measured iteration of the emitter throughput benchmark.
const THROUGHPUT_DECISIONS: usize = 10_000;

const METADATA_JSON: &[u8] = br#"{
    "cedar_version": "4.4.0",
    "policy_store": {
        "id": "7ace0b1d2f40",
        "name": "TRACE Benchmark",
        "version": "1.0.0"
    }
}"#;

const SCHEMA: &[u8] = br#"namespace TestApp {
    entity User;
    entity Resource;
    action "read" appliesTo {
        principal: [User],
        resource: [Resource]
    };
}"#;

const POLICY: &[u8] = br#"@id("allow-read")
permit(principal, action == TestApp::Action::"read", resource);"#;

/// A `.cjar` store: TRACE records carry the `bundle_hash` of its bytes, which
/// only an archive has.
fn policy_store_archive() -> Vec<u8> {
    let mut zip = ZipWriter::new(Cursor::new(Vec::new()));
    let options = FileOptions::<ExtendedFileOptions>::default()
        .compression_method(CompressionMethod::Deflated);
    for file in [
        ArchiveFile {
            path: "metadata.json",
            content: METADATA_JSON,
        },
        ArchiveFile {
            path: "schema.cedarschema",
            content: SCHEMA,
        },
        ArchiveFile {
            path: "policies/allow-read.cedar",
            content: POLICY,
        },
    ] {
        zip.start_file(file.path, options.clone())
            .expect("archive entry should start");
        zip.write_all(file.content)
            .expect("archive entry should be written");
    }
    zip.finish()
        .expect("archive should be finished")
        .into_inner()
}

struct ArchiveFile {
    path: &'static str,
    content: &'static [u8],
}

/// A mock Lock that accepts every record and counts the records it received.
struct MockLock {
    server: ServerGuard,
    received: Arc<AtomicUsize>,
}

impl MockLock {
    async fn start() -> Self {
        let mut server = Server::new_async().await;
        let base = server.url();
        server
            .mock("GET", "/.well-known/lock-server-configuration")
            .with_body(
                json!({
                    "version": "1.0",
                    "issuer": base,
                    "audit": {
                        "log_endpoint": format!("{base}/audit/log"),
                        "health_endpoint": format!("{base}/audit/health"),
                        "telemetry_endpoint": format!("{base}/audit/telemetry"),
                        "trace_bulk_endpoint": format!("{base}{BULK_PATH}"),
                    },
                    "config": {
                        "config_endpoint": format!("{base}/config"),
                        "issuers_endpoint": format!("{base}/config/issuers"),
                        "policy_endpoint": format!("{base}/config/policy"),
                        "schema_endpoint": format!("{base}/config/schema"),
                        "sse_endpoint": format!("{base}/sse"),
                    },
                })
                .to_string(),
            )
            .create_async()
            .await;

        let received = Arc::new(AtomicUsize::new(0));
        let counter = received.clone();
        server
            .mock("POST", BULK_PATH)
            .with_status(207)
            .with_body_from_request(move |request| {
                let count =
                    serde_json::from_slice::<Vec<IgnoredAny>>(request.body().expect("a bulk body"))
                        .expect("the bulk body is an array of records")
                        .len();
                counter.fetch_add(count, Ordering::SeqCst);
                let results: Vec<_> = (0..count)
                    .map(|index| json!({"index": index, "accepted": true}))
                    .collect();
                json!({ "results": results }).to_string().into_bytes()
            })
            .expect_at_least(0)
            .create_async()
            .await;
        Self { server, received }
    }

    fn received(&self) -> usize {
        self.received.load(Ordering::SeqCst)
    }

    async fn wait_received(&self, target: usize) {
        while self.received() < target {
            tokio::time::sleep(Duration::from_millis(1)).await;
        }
    }

    fn lock_config(&self, log_channel_capacity: NonZeroUsize) -> LockServiceConfig {
        LockServiceConfig {
            config_uri: format!(
                "{}/.well-known/lock-server-configuration",
                self.server.url()
            )
            .parse()
            .expect("mock Lock URL should parse"),
            access_token_jwt: Some("pre.issued.token".to_string()), // # gitleaks:allow
            log_channel_capacity,
            ..Default::default()
        }
    }
}

fn trace_config() -> TraceConfig {
    TraceConfig {
        producer_id: "cedarling-bench".parse().expect("valid producer id"),
        signing_key: TraceSigningKey::File(
            Path::new(env!("CARGO_MANIFEST_DIR")).join("../test_files/trace/producer_ed25519.pem"),
        ),
        signing_kid: None,
        endpoint: TraceEndpoint::Discovered,
        bulk_max_records: NonZeroUsize::new(100).expect("non-zero"),
        bulk_max_bytes: NonZeroUsize::new(4_194_304).expect("non-zero"),
    }
}

/// Metrics are collected in every variant, so the TRACE counters can be read
/// and the variants pay the same metrics cost.
fn bootstrap_config(lock: Option<LockServiceConfig>, trace: bool) -> BootstrapConfig {
    BootstrapConfig {
        application_name: "trace_bench".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::Off,
            log_level: LogLevel::DEBUG,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::ArchiveBytes(policy_store_archive()),
            ..Default::default()
        },
        jwt_config: JwtConfig::new_without_validation(),
        authorization_config: AuthorizationConfig {
            metrics_collection: true,
            ..Default::default()
        },
        lock_config: lock,
        max_base64_size: None,
        max_default_entities: None,
        data_store_config: DataStoreConfig::default(),
        http_client_config: HttpClientConfig::default(),
        trace_config: trace.then(trace_config),
    }
}

fn entity(entity_type: &str, id: &str) -> EntityData {
    EntityData::from_json(
        &json!({"cedar_entity_mapping": {"entity_type": entity_type, "id": id}}).to_string(),
    )
    .expect("benchmark entity data")
}

fn request(options: AuthorizeOptions) -> RequestUnsigned {
    RequestUnsigned {
        principal: Some(entity("TestApp::User", "u1")),
        action: "TestApp::Action::\"read\"".to_string(),
        resource: entity("TestApp::Resource", "r1"),
        context: json!({}),
        options,
    }
}

fn with_trace() -> AuthorizeOptions {
    AuthorizeOptions::with_trace(TraceContext::new(
        "exec-bench".to_string(),
        "https://gateway.example.com".to_string(),
        "workload-bench".to_string(),
    ))
}

/// A TRACE counter from the metrics drained since the previous call.
fn drain_trace_counter(cedarling: &Cedarling, name: &str) -> i64 {
    cedarling
        .drain_metrics()
        .expect("metrics are collected")
        .operational_stats
        .get(name)
        .copied()
        .unwrap_or_default()
}

/// One decision before measuring, so a benchmark never times a path other
/// than the one it names.
async fn expect_status(cedarling: &Cedarling, options: AuthorizeOptions, status: &str) {
    cedarling.drain_metrics().expect("metrics are collected");
    let result = cedarling
        .authorize_unsigned(request(options))
        .await
        .expect("the decision should be made");
    assert!(result.decision, "the benchmark policy allows the request");
    assert_eq!(
        drain_trace_counter(cedarling, status),
        1,
        "the decision should end with {status}"
    );
}

struct Variant {
    name: &'static str,
    trace: bool,
    options: fn() -> AuthorizeOptions,
    status: Option<&'static str>,
}

fn trace_authorize_unsigned(c: &mut Criterion) {
    let runtime = Runtime::new().expect("init tokio runtime");
    let lock = runtime.block_on(MockLock::start());
    let mut group = c.benchmark_group("trace_authorize_unsigned");

    for variant in [
        Variant {
            name: "disabled",
            trace: false,
            options: with_trace,
            status: None,
        },
        Variant {
            name: "enabled_with_context",
            trace: true,
            options: with_trace,
            status: Some("trace.emitted"),
        },
        Variant {
            name: "enabled_missing_context",
            trace: true,
            options: AuthorizeOptions::default,
            status: Some("trace.not_emitted_missing_context"),
        },
    ] {
        // The default queue capacity, so a queue that cannot keep up shows as
        // `queue_full` in the counts printed below.
        let config = bootstrap_config(
            Some(lock.lock_config(LockServiceConfig::default().log_channel_capacity)),
            variant.trace,
        );
        let cedarling = runtime
            .block_on(Cedarling::new(&config))
            .expect("Cedarling should start");
        if let Some(status) = variant.status {
            runtime.block_on(expect_status(&cedarling, (variant.options)(), status));
        }

        let request = request((variant.options)());
        group.bench_function(variant.name, |b| {
            b.to_async(&runtime)
                .iter(|| cedarling.authorize_unsigned(black_box(request.clone())));
        });

        runtime.block_on(cedarling.shut_down());
        let metrics = cedarling.drain_metrics().expect("metrics are collected");
        let count = |name: &str| {
            metrics
                .operational_stats
                .get(name)
                .copied()
                .unwrap_or_default()
        };
        eprintln!(
            "{}: emitted {}, queue_full {}, accepted {}",
            variant.name,
            count("trace.emitted"),
            count("trace.not_emitted_queue_full"),
            count("trace.accepted"),
        );
    }
    group.finish();
}

/// Time from the first of `THROUGHPUT_DECISIONS` decisions until Lock has
/// received a record for each. The queue holds all of them, so the emitter is
/// measured rather than records dropped at the queue.
fn trace_emitter_throughput(c: &mut Criterion) {
    let runtime = Runtime::new().expect("init tokio runtime");
    let lock = runtime.block_on(MockLock::start());
    // One more slot for the record of the decision made before measuring.
    let capacity = NonZeroUsize::new(THROUGHPUT_DECISIONS + 1).expect("non-zero");
    let cedarling = runtime
        .block_on(Cedarling::new(&bootstrap_config(
            Some(lock.lock_config(capacity)),
            true,
        )))
        .expect("Cedarling should start");
    runtime.block_on(expect_status(&cedarling, with_trace(), "trace.emitted"));
    // Lock has that record before the first sample counts what it receives.
    runtime.block_on(lock.wait_received(1));
    let request = request(with_trace());

    let mut group = c.benchmark_group("trace_emitter_throughput");
    group.sample_size(10);
    group.throughput(Throughput::Elements(THROUGHPUT_DECISIONS as u64));
    group.bench_function("n10000", |b| {
        b.to_async(&runtime).iter_custom(|iters| {
            let cedarling = &cedarling;
            let lock = &lock;
            let request = &request;
            async move {
                let mut total = Duration::ZERO;
                for _ in 0..iters {
                    let target = lock.received() + THROUGHPUT_DECISIONS;
                    let start = Instant::now();
                    for _ in 0..THROUGHPUT_DECISIONS {
                        cedarling
                            .authorize_unsigned(request.clone())
                            .await
                            .expect("the decision should be made");
                    }
                    lock.wait_received(target).await;
                    total += start.elapsed();
                }
                total
            }
        });
    });
    group.finish();

    assert_eq!(
        drain_trace_counter(&cedarling, "trace.not_emitted_queue_full"),
        0,
        "the queue should hold every decision of an iteration"
    );
    runtime.block_on(cedarling.shut_down());
}

fn measurement_config() -> Criterion {
    Criterion::default()
        .measurement_time(Duration::from_secs(5))
        .warm_up_time(Duration::from_secs(3))
}

criterion_group! {
    name = trace_benchmark;
    config = measurement_config();
    targets = trace_authorize_unsigned, trace_emitter_throughput
}

criterion_main!(trace_benchmark);
