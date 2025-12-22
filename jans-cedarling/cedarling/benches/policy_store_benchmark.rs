// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy store loading and validation benchmarks.
//!
//! Run with: `cargo bench --bench policy_store_benchmark`

use std::hint::black_box as bb;
use std::io::{Cursor, Write};

use criterion::{BenchmarkId, Criterion, criterion_group, criterion_main};
use tempfile::TempDir;
use zip::write::{ExtendedFileOptions, FileOptions};
use zip::{CompressionMethod, ZipWriter};

/// Create a minimal valid policy store archive for benchmarking.
fn create_minimal_archive() -> Vec<u8> {
    create_archive_with_policies(1)
}

/// Create a policy store archive with the specified number of policies.
fn create_archive_with_policies(policy_count: usize) -> Vec<u8> {
    let buffer = Vec::new();
    let cursor = Cursor::new(buffer);
    let mut zip = ZipWriter::new(cursor);
    let options = FileOptions::<ExtendedFileOptions>::default()
        .compression_method(CompressionMethod::Deflated);

    // metadata.json
    zip.start_file("metadata.json", options.clone()).unwrap();
    zip.write_all(
        br#"{
        "cedar_version": "4.4.0",
        "policy_store": {
            "id": "bench123456789",
            "name": "Benchmark Policy Store",
            "version": "1.0.0"
        }
    }"#,
    )
    .unwrap();

    // schema.cedarschema
    zip.start_file("schema.cedarschema", options.clone())
        .unwrap();
    zip.write_all(
        br#"namespace TestApp {
    entity User;
    entity Resource;
    action "read" appliesTo {
        principal: [User],
        resource: [Resource]
    };
}"#,
    )
    .unwrap();

    // policies
    for i in 0..policy_count {
        let filename = format!("policies/policy{}.cedar", i);
        zip.start_file(&filename, options.clone()).unwrap();
        let policy = format!(
            r#"@id("policy{}")
permit(
    principal == TestApp::User::"user{}",
    action == TestApp::Action::"read",
    resource == TestApp::Resource::"res{}"
);"#,
            i, i, i
        );
        zip.write_all(policy.as_bytes()).unwrap();
    }

    zip.finish().unwrap().into_inner()
}

/// Create a policy store archive with the specified number of entities.
fn create_archive_with_entities(entity_count: usize) -> Vec<u8> {
    let buffer = Vec::new();
    let cursor = Cursor::new(buffer);
    let mut zip = ZipWriter::new(cursor);
    let options = FileOptions::<ExtendedFileOptions>::default()
        .compression_method(CompressionMethod::Deflated);

    // metadata.json
    zip.start_file("metadata.json", options.clone()).unwrap();
    zip.write_all(
        br#"{
        "cedar_version": "4.4.0",
        "policy_store": {
            "id": "bench123456789",
            "name": "Benchmark Policy Store",
            "version": "1.0.0"
        }
    }"#,
    )
    .unwrap();

    // schema.cedarschema
    zip.start_file("schema.cedarschema", options.clone())
        .unwrap();
    zip.write_all(
        br#"namespace TestApp {
    entity User {
        name: String,
        email: String,
    };
    entity Resource;
    action "read" appliesTo {
        principal: [User],
        resource: [Resource]
    };
}"#,
    )
    .unwrap();

    // One policy
    zip.start_file("policies/allow.cedar", options.clone())
        .unwrap();
    zip.write_all(br#"@id("allow") permit(principal, action, resource);"#)
        .unwrap();

    // Entities in batches
    let batch_size = 500;
    let batches = (entity_count + batch_size - 1) / batch_size;

    for batch in 0..batches {
        let start = batch * batch_size;
        let end = ((batch + 1) * batch_size).min(entity_count);

        let mut entities = Vec::new();
        for i in start..end {
            entities.push(format!(
                r#"{{"uid":{{"type":"TestApp::User","id":"user{}"}},"attrs":{{"name":"User {}","email":"user{}@example.com"}},"parents":[]}}"#,
                i, i, i
            ));
        }

        let filename = format!("entities/users_batch{}.json", batch);
        zip.start_file(&filename, options.clone()).unwrap();
        let content = format!("[{}]", entities.join(","));
        zip.write_all(content.as_bytes()).unwrap();
    }

    zip.finish().unwrap().into_inner()
}

/// Benchmark loading a minimal policy store.
///
/// Note: This benchmark measures archive decompression and parsing overhead,
/// not the full Cedarling initialization which involves more complex setup.
fn bench_archive_parsing(c: &mut Criterion) {
    let archive = create_minimal_archive();

    c.bench_function("archive_parse_minimal", |b| {
        b.iter(|| {
            // Measure ZIP parsing overhead
            let cursor = Cursor::new(bb(archive.clone()));
            let archive = zip::ZipArchive::new(cursor).unwrap();
            bb(archive.len())
        })
    });
}

/// Benchmark archive creation with varying policy counts.
fn bench_archive_creation(c: &mut Criterion) {
    let mut group = c.benchmark_group("archive_creation");

    for policy_count in [10, 50, 100, 500].iter() {
        group.bench_with_input(
            BenchmarkId::new("policies", policy_count),
            policy_count,
            |b, &count| b.iter(|| bb(create_archive_with_policies(count))),
        );
    }

    group.finish();
}

/// Benchmark archive parsing with varying policy counts.
fn bench_archive_parsing_policies(c: &mut Criterion) {
    let mut group = c.benchmark_group("archive_parse_policies");

    for policy_count in [10, 50, 100, 500].iter() {
        let archive = create_archive_with_policies(*policy_count);

        group.bench_with_input(
            BenchmarkId::new("parse", policy_count),
            &archive,
            |b, archive| {
                b.iter(|| {
                    let cursor = Cursor::new(bb(archive.clone()));
                    let mut zip = zip::ZipArchive::new(cursor).unwrap();

                    // Read all files to simulate loading
                    let mut total_size = 0;
                    for i in 0..zip.len() {
                        let mut file = zip.by_index(i).unwrap();
                        let bytes_read = std::io::copy(&mut file, &mut std::io::sink()).unwrap();
                        total_size += bytes_read;
                    }
                    bb(total_size)
                })
            },
        );
    }

    group.finish();
}

/// Benchmark archive parsing with varying entity counts.
fn bench_archive_parsing_entities(c: &mut Criterion) {
    let mut group = c.benchmark_group("archive_parse_entities");

    for entity_count in [100, 500, 1000, 5000].iter() {
        let archive = create_archive_with_entities(*entity_count);

        group.bench_with_input(
            BenchmarkId::new("parse", entity_count),
            &archive,
            |b, archive| {
                b.iter(|| {
                    let cursor = Cursor::new(bb(archive.clone()));
                    let mut zip = zip::ZipArchive::new(cursor).unwrap();

                    // Read all files to simulate loading
                    let mut total_size = 0;
                    for i in 0..zip.len() {
                        let mut file = zip.by_index(i).unwrap();
                        let bytes_read = std::io::copy(&mut file, &mut std::io::sink()).unwrap();
                        total_size += bytes_read;
                    }
                    bb(total_size)
                })
            },
        );
    }

    group.finish();
}

/// Benchmark directory creation (native only).
#[cfg(not(target_arch = "wasm32"))]
fn bench_directory_creation(c: &mut Criterion) {
    use std::fs;

    let mut group = c.benchmark_group("directory_creation");

    for policy_count in [10, 50, 100].iter() {
        group.bench_with_input(
            BenchmarkId::new("policies", policy_count),
            policy_count,
            |b, &count| {
                b.iter(|| {
                    let temp_dir = TempDir::new().unwrap();
                    let dir = temp_dir.path();

                    // Create metadata.json
                    fs::write(
                        dir.join("metadata.json"),
                        r#"{"cedar_version":"4.4.0","policy_store":{"id":"bench","name":"Bench","version":"1.0.0"}}"#,
                    )
                    .unwrap();

                    // Create schema
                    fs::write(
                        dir.join("schema.cedarschema"),
                        "namespace TestApp { entity User; entity Resource; }",
                    )
                    .unwrap();

                    // Create policies directory
                    fs::create_dir(dir.join("policies")).unwrap();

                    for i in 0..count {
                        let policy = format!(
                            r#"@id("policy{}") permit(principal, action, resource);"#,
                            i
                        );
                        fs::write(dir.join(format!("policies/policy{}.cedar", i)), policy).unwrap();
                    }

                    bb(temp_dir)
                })
            },
        );
    }

    group.finish();
}

criterion_group!(
    benches,
    bench_archive_parsing,
    bench_archive_creation,
    bench_archive_parsing_policies,
    bench_archive_parsing_entities,
);

#[cfg(not(target_arch = "wasm32"))]
criterion_group!(directory_benches, bench_directory_creation,);

#[cfg(not(target_arch = "wasm32"))]
criterion_main!(benches, directory_benches);

#[cfg(target_arch = "wasm32")]
criterion_main!(benches);
