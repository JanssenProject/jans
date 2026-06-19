// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

fn main() {
    #[cfg(feature = "grpc")]
    compile_protos();
    emit_build_metadata();
}

fn emit_build_metadata() {
    git_rerun_if_changed();
    let commit = std::env::var("CEDARLING_BUILD_COMMIT").unwrap_or_else(|_| {
        std::process::Command::new("git")
            .args(["rev-parse", "--short", "HEAD"])
            .output()
            .ok()
            .and_then(|o| {
                if o.status.success() {
                    Some(String::from_utf8_lossy(&o.stdout).trim().to_string())
                } else {
                    None
                }
            })
            .unwrap_or_default()
    });

    let timestamp = std::env::var("CEDARLING_BUILD_TIMESTAMP").unwrap_or_else(|_| {
        chrono::Utc::now().to_rfc3339()
    });

    println!("cargo:rustc-env=CEDARLING_BUILD_COMMIT={commit}");
    println!("cargo:rustc-env=CEDARLING_BUILD_TIMESTAMP={timestamp}");
}

/// Emit `rerun-if-changed` for relevant git files so Cargo rebuilds when HEAD
/// moves. Uses `git rev-parse --git-dir` to reliably locate `.git` regardless
/// of worktrees, submodules, or workspace nesting.
fn git_rerun_if_changed() {
    let manifest_dir = match std::env::var("CARGO_MANIFEST_DIR") {
        Ok(d) => d,
        Err(_) => return,
    };

    let git_dir_output = std::process::Command::new("git")
        .args(["-C", &manifest_dir, "rev-parse", "--git-dir"])
        .output()
        .ok()
        .filter(|o| o.status.success());

    let git_dir = match git_dir_output {
        Some(ref o) => {
            let path = String::from_utf8_lossy(&o.stdout).trim().to_string();
            let p = std::path::Path::new(&path);
            if p.is_absolute() {
                p.to_path_buf()
            } else {
                std::path::Path::new(&manifest_dir).join(p)
            }
        },
        None => return,
    };

    let head = git_dir.join("HEAD");
    if head.exists() {
        println!("cargo:rerun-if-changed={}", head.display());

        if let Ok(content) = std::fs::read_to_string(&head) {
            if let Some(ref_path) = content.strip_prefix("ref: ") {
                let ref_file = git_dir.join(ref_path.trim());
                if ref_file.exists() {
                    println!("cargo:rerun-if-changed={}", ref_file.display());
                }
            }
        }
    }

    let packed_refs = git_dir.join("packed-refs");
    if packed_refs.exists() {
        println!("cargo:rerun-if-changed={}", packed_refs.display());
    }
}

#[cfg(feature = "grpc")]
fn compile_protos() {
    let audit_proto = "src/lock/proto/audit.proto";

    println!("cargo:rerun-if-changed={audit_proto}");

    tonic_prost_build::configure()
        .build_transport(false)
        .compile_protos(&[audit_proto], &["src/lock/proto/"])
        .expect("Failed to compile protos");
}
