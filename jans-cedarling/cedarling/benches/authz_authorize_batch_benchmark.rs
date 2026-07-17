// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Batch vs. sequence-of-single-item benchmarks for both authorize flows,
//! at N = 10 and N = 25. Timings are per whole batch / whole sequence.

use std::collections::HashSet;
use std::time::Duration;

use cedarling::{
    AuthorizationConfig, AuthorizeMultiIssuerRequest, BatchAuthorizeMultiIssuerRequest,
    BatchAuthorizeUnsignedRequest, BatchItem, BootstrapConfig, Cedarling, DataStoreConfig,
    EntityData, HttpClientConfig, InitCedarlingError, JwtConfig, LogConfig, LogLevel,
    LogTypeConfig, PolicyStoreConfig, PolicyStoreSource, RequestUnsigned, TokenInput,
};
use criterion::{
    BatchSize, BenchmarkGroup, BenchmarkId, Criterion, criterion_group, criterion_main,
};
use criterion::measurement::WallTime;
use jsonwebtoken::Algorithm;
use serde::Deserialize;
use serde_json::{Value, json};
use test_utils::MockServer;
use test_utils::token_claims::generate_token_using_claims_and_keypair;
use test_utils::{gen_mock_server};
use tokio::runtime::Runtime;

const UNSIGNED_POLICY_STORE: &str =
    include_str!("../../test_files/policy-store_ok_2.yaml");
const MULTI_ISSUER_POLICY_STORE: &str =
    include_str!("../../test_files/policy-store-multi-issuer-basic.yaml");

const BATCH_SIZES: [usize; 2] = [10, 25];

// ─── Unsigned ───────────────────────────────────────────────────────

fn unsigned_batch_vs_sequence(c: &mut Criterion) {
    let runtime = Runtime::new().expect("init tokio runtime");
    let cedarling = runtime
        .block_on(prepare_unsigned_cedarling())
        .expect("init cedarling (unsigned)");
    let template = prepare_unsigned_request();

    for &n in &BATCH_SIZES {
        let mut group = c.benchmark_group(format!("unsigned_batch_vs_sequence_n{n}"));
        group.measurement_time(Duration::from_secs(5));
        group.warm_up_time(Duration::from_secs(3));

        let items: Vec<BatchItem> = (0..n)
            .map(|i| build_batch_item(&template, i))
            .collect();
        let batch_request = BatchAuthorizeUnsignedRequest::new(
            template.principal.clone(),
            items.clone(),
        );
        let sequence_requests: Vec<RequestUnsigned> = items
            .iter()
            .map(|item| RequestUnsigned {
                principal: template.principal.clone(),
                action: item.action.clone(),
                resource: item.resource.clone(),
                context: item.context.clone(),
            })
            .collect();

        bench_unsigned_batch(&mut group, &runtime, &cedarling, &batch_request, n);
        bench_unsigned_sequence(&mut group, &runtime, &cedarling, &sequence_requests, n);
        group.finish();
    }
}

fn bench_unsigned_batch(
    group: &mut BenchmarkGroup<'_, WallTime>,
    runtime: &Runtime,
    cedarling: &Cedarling,
    request: &BatchAuthorizeUnsignedRequest,
    n: usize,
) {
    // Pre-bench trial catches fixture drift before timing begins.
    let trial = runtime
        .block_on(cedarling.authorize_unsigned_batch(request.clone()))
        .expect("trial batch call");
    assert_eq!(trial.results.len(), n, "batch size must match");
    assert!(
        trial.results.iter().all(|r| r.decision),
        "all batch items must Allow — check fixtures"
    );

    group.bench_with_input(BenchmarkId::new("batch", n), request, |b, req| {
        b.to_async(runtime).iter_batched(
            || req.clone(),
            |owned| async move {
                cedarling
                    .authorize_unsigned_batch(owned)
                    .await
                    .expect("batch call succeeds")
            },
            BatchSize::SmallInput,
        );
    });
}

fn bench_unsigned_sequence(
    group: &mut BenchmarkGroup<'_, WallTime>,
    runtime: &Runtime,
    cedarling: &Cedarling,
    requests: &[RequestUnsigned],
    n: usize,
) {
    for r in requests {
        let trial = runtime
            .block_on(cedarling.authorize_unsigned(r.clone()))
            .expect("trial single call");
        assert!(
            trial.decision,
            "all sequence items must Allow — check fixtures"
        );
    }

    group.bench_with_input(BenchmarkId::new("sequence", n), requests, |b, reqs| {
        b.to_async(runtime).iter_batched(
            || reqs.to_vec(),
            |owned| async move {
                for r in owned {
                    let _ = cedarling
                        .authorize_unsigned(r)
                        .await
                        .expect("single call succeeds");
                }
            },
            BatchSize::SmallInput,
        );
    });
}

// ─── Multi-issuer ───────────────────────────────────────────────────

fn multi_issuer_batch_vs_sequence(c: &mut Criterion) {
    let mock1 = gen_mock_server();
    let mock2 = gen_mock_server();
    let runtime = Runtime::new().expect("init tokio runtime");
    let cedarling = runtime
        .block_on(prepare_multi_issuer_cedarling(
            &mock1.base_idp_url,
            &mock2.base_idp_url,
        ))
        .expect("init cedarling (multi-issuer)");
    let template = prepare_multi_issuer_request(&mock1, &mock2);

    for &n in &BATCH_SIZES {
        let mut group = c.benchmark_group(format!("multi_issuer_batch_vs_sequence_n{n}"));
        group.measurement_time(Duration::from_secs(5));
        group.warm_up_time(Duration::from_secs(3));

        let items: Vec<BatchItem> = (0..n)
            .map(|i| build_batch_item_multi_issuer(&template, i))
            .collect();
        let tokens = template.tokens.clone();
        let batch_request = BatchAuthorizeMultiIssuerRequest::new(tokens.clone(), items.clone());
        let sequence_requests: Vec<AuthorizeMultiIssuerRequest> = items
            .iter()
            .map(|item| {
                AuthorizeMultiIssuerRequest::new_with_fields(
                    tokens.clone(),
                    item.resource.clone(),
                    item.action.clone(),
                    Some(item.context.clone()),
                )
            })
            .collect();

        bench_multi_issuer_batch(&mut group, &runtime, &cedarling, &batch_request, n);
        bench_multi_issuer_sequence(&mut group, &runtime, &cedarling, &sequence_requests, n);
        group.finish();
    }
}

fn bench_multi_issuer_batch(
    group: &mut BenchmarkGroup<'_, WallTime>,
    runtime: &Runtime,
    cedarling: &Cedarling,
    request: &BatchAuthorizeMultiIssuerRequest,
    n: usize,
) {
    let trial = runtime
        .block_on(cedarling.authorize_multi_issuer_batch(request.clone()))
        .expect("trial batch call");
    assert_eq!(trial.results.len(), n, "batch size must match");
    assert!(
        trial.results.iter().all(|r| r.decision),
        "all batch items must Allow — check fixtures"
    );

    group.bench_with_input(BenchmarkId::new("batch", n), request, |b, req| {
        b.to_async(runtime).iter_batched(
            || req.clone(),
            |owned| async move {
                cedarling
                    .authorize_multi_issuer_batch(owned)
                    .await
                    .expect("batch call succeeds")
            },
            BatchSize::SmallInput,
        );
    });
}

fn bench_multi_issuer_sequence(
    group: &mut BenchmarkGroup<'_, WallTime>,
    runtime: &Runtime,
    cedarling: &Cedarling,
    requests: &[AuthorizeMultiIssuerRequest],
    n: usize,
) {
    for r in requests {
        let trial = runtime
            .block_on(cedarling.authorize_multi_issuer(r.clone()))
            .expect("trial single call");
        assert!(
            trial.decision,
            "all sequence items must Allow — check fixtures"
        );
    }

    group.bench_with_input(BenchmarkId::new("sequence", n), requests, |b, reqs| {
        b.to_async(runtime).iter_batched(
            || reqs.to_vec(),
            |owned| async move {
                for r in owned {
                    let _ = cedarling
                        .authorize_multi_issuer(r)
                        .await
                        .expect("single call succeeds");
                }
            },
            BatchSize::SmallInput,
        );
    });
}

// ─── Helpers ─────────────────────────────────────────────────────────

fn build_batch_item(template: &RequestUnsigned, i: usize) -> BatchItem {
    let mut resource = template.resource.clone();
    resource.cedar_mapping.id = format!("resource-{i}");
    BatchItem {
        resource,
        action: template.action.clone(),
        context: template.context.clone(),
    }
}

fn build_batch_item_multi_issuer(template: &AuthorizeMultiIssuerRequest, _i: usize) -> BatchItem {
    // Fixture policy scopes to a single `Acme::Resource::"WikiPages"`, so every
    // item reuses that resource. Batch loop still runs N Cedar evaluations.
    BatchItem {
        resource: template.resource.clone(),
        action: template.action.clone(),
        context: template.context.clone().unwrap_or_else(|| json!({})),
    }
}

async fn prepare_unsigned_cedarling() -> Result<Cedarling, InitCedarlingError> {
    let bootstrap_config = BootstrapConfig {
        application_name: "batch_bench".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::Off,
            log_level: LogLevel::INFO,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(UNSIGNED_POLICY_STORE.to_string()),
            ..Default::default()
        },
        jwt_config: JwtConfig::new_without_validation(),
        authorization_config: AuthorizationConfig::default(),
        lock_config: None,
        max_base64_size: None,
        max_default_entities: None,
        data_store_config: DataStoreConfig::default(),
        http_client_config: HttpClientConfig::default(),
    };
    Cedarling::new(&bootstrap_config).await
}

fn prepare_unsigned_request() -> RequestUnsigned {
    let principal = EntityData::deserialize(json!({
        "cedar_entity_mapping": {
            "entity_type": "Jans::TestPrincipal1",
            "id": "bench_principal"
        },
        "is_ok": true
    }))
    .expect("valid principal");
    let resource = EntityData::deserialize(json!({
        "cedar_entity_mapping": {
            "entity_type": "Jans::Issue",
            "id": "resource-template"
        },
        "org_id": "some_long_id",
        "country": "US"
    }))
    .expect("valid resource");
    RequestUnsigned {
        principal: Some(principal),
        action: "Jans::Action::\"UpdateForTestPrincipals\"".to_string(),
        context: json!({}),
        resource,
    }
}

async fn prepare_multi_issuer_cedarling(
    base_idp_url1: &str,
    base_idp_url2: &str,
) -> Result<Cedarling, InitCedarlingError> {
    let mut policy_store: serde_yaml_ng::Value =
        serde_yaml_ng::from_str(MULTI_ISSUER_POLICY_STORE).expect("valid YAML");
    policy_store["policy_stores"]["multi_issuer_basic_store"]["trusted_issuers"]["AcmeIssuer"]
        ["openid_configuration_endpoint"] =
        format!("{base_idp_url1}/.well-known/openid-configuration").into();
    policy_store["policy_stores"]["multi_issuer_basic_store"]["trusted_issuers"]["DolphinIssuer"]
        ["openid_configuration_endpoint"] =
        format!("{base_idp_url2}/.well-known/openid-configuration").into();

    let bootstrap_config = BootstrapConfig {
        application_name: "batch_bench_mi".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::Off,
            log_level: LogLevel::INFO,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(
                serde_yaml_ng::to_string(&policy_store).expect("serialize YAML"),
            ),
            ..Default::default()
        },
        jwt_config: JwtConfig {
            jwks: None,
            jwt_sig_validation: true,
            jwt_status_validation: false,
            signature_algorithms_supported: HashSet::from([Algorithm::HS256]),
            ..Default::default()
        },
        authorization_config: AuthorizationConfig::default(),
        lock_config: None,
        max_base64_size: None,
        max_default_entities: None,
        data_store_config: DataStoreConfig::default(),
        http_client_config: HttpClientConfig::default(),
    };
    Cedarling::new(&bootstrap_config).await
}

fn prepare_multi_issuer_request(
    mock1: &MockServer,
    mock2: &MockServer,
) -> AuthorizeMultiIssuerRequest {
    let acme_token = generate_token_using_claims_and_keypair(
        &claim_set(&mock1.base_idp_url, "acme_bench_sub", "acme_client_bench"),
        &mock1.keys,
    );
    let dolphin_token = generate_token_using_claims_and_keypair(
        &claim_set(&mock2.base_idp_url, "dolphin_bench_sub", "dolphin_client_bench"),
        &mock2.keys,
    );
    AuthorizeMultiIssuerRequest::new_with_fields(
        vec![
            TokenInput::new("Acme::Access_Token".to_string(), acme_token),
            TokenInput::new("Dolphin::Access_Token".to_string(), dolphin_token),
        ],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": { "entity_type": "Acme::Resource", "id": "WikiPages" },
                "name": "Batch Bench Resource"
            })
            .to_string(),
        )
        .expect("resource entity"),
        "Acme::Action::\"ReadProfile\"".to_string(),
        None,
    )
}

fn claim_set(iss: &str, sub: &str, client_id: &str) -> Value {
    json!({
        "iss": iss,
        "sub": sub,
        "jti": format!("{sub}-jti"),
        "client_id": client_id,
        "aud": format!("{client_id}-audience"),
        "scope": ["read:wiki"],
        "exp": 2_000_000_000,
        "iat": 1_516_239_022,
    })
}

fn measurement_config() -> Criterion {
    Criterion::default()
        .measurement_time(Duration::from_secs(5))
        .warm_up_time(Duration::from_secs(3))
}

criterion_group! {
    name = authz_batch_benchmark;
    config = measurement_config();
    targets = unsigned_batch_vs_sequence, multi_issuer_batch_vs_sequence
}

criterion_main!(authz_batch_benchmark);
