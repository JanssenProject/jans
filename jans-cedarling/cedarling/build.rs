// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

fn main() {
    #[cfg(feature = "grpc")]
    compile_protos();
    emit_build_metadata();
}

/// Embeds the current git commit hash and build timestamp as environment
/// variables (`CEDARLING_BUILD_COMMIT`, `CEDARLING_BUILD_TIMESTAMP`) so they
/// are available at compile time via `env!()`.
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

/// Emits `rerun-if-changed` for `HEAD`, the resolved branch ref, and
/// `packed-refs` so Cargo rebuilds when the checked-out commit changes.
///
/// Uses `--git-dir` / `--git-common-dir` so the lookup works correctly in
/// linked worktrees, submodules, and nested workspaces.
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

    // In linked worktrees `--git-dir` points to `.git/worktrees/<name>`, but
    // branch refs and `packed-refs` reside in the common directory
    // (`.git/`).  `--git-common-dir` returns that shared location, or the
    // same path as `--git-dir` for a normal repository.
    let common_dir = std::process::Command::new("git")
        .args(["-C", &manifest_dir, "rev-parse", "--git-common-dir"])
        .output()
        .ok()
        .filter(|o| o.status.success())
        .and_then(|o| {
            let path = String::from_utf8_lossy(&o.stdout).trim().to_string();
            if path.is_empty() {
                return None;
            }
            let p = std::path::Path::new(&path);
            let abs = if p.is_absolute() {
                p.to_path_buf()
            } else {
                std::path::Path::new(&manifest_dir).join(p)
            };
            // Only keep the common dir when it differs from the worktree dir
            (abs != git_dir).then_some(abs)
        })
        // Fall back to `git_dir` when the command fails or returns the same
        // path (plain repos, detached HEAD, etc.)
        .unwrap_or_else(|| git_dir.clone());

    let head = git_dir.join("HEAD");
    if head.exists() {
        println!("cargo:rerun-if-changed={}", head.display());

        if let Ok(content) = std::fs::read_to_string(&head) {
            if let Some(ref_path) = content.strip_prefix("ref: ") {
                let ref_file = git_dir.join(ref_path.trim());
                let candidate = ref_file.exists().then_some(ref_file)
                    .or_else(|| {
                        let f = common_dir.join(ref_path.trim());
                        f.exists().then_some(f)
                    });
                if let Some(f) = candidate {
                    println!("cargo:rerun-if-changed={}", f.display());
                }
            }
        }
    }

    let packed_refs = git_dir.join("packed-refs");
    let candidate = packed_refs.exists().then_some(packed_refs)
        .or_else(|| {
            let f = common_dir.join("packed-refs");
            f.exists().then_some(f)
        });
    if let Some(f) = candidate {
        println!("cargo:rerun-if-changed={}", f.display());
    }
}

/// Compiles the audit protobuf definition into Rust code via `tonic-build`.
#[cfg(feature = "grpc")]
fn compile_protos() {
    let audit_proto = "src/lock/proto/audit.proto";

    println!("cargo:rerun-if-changed={audit_proto}");

    tonic_prost_build::configure()
        .build_transport(false)
        .compile_protos(&[audit_proto], &["src/lock/proto/"])
        .expect("Failed to compile protos");
}
