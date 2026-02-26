// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

fn main() {
    #[cfg(feature = "grpc")]
    compile_protos();
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
