fn main() {
    let cwd = std::env::var("CARGO_MANIFEST_DIR").unwrap();

    // fetch cedarling version from its Cargo.toml
    let cedarling_cargo_toml = "../../../../cedarling/Cargo.toml";
    let meta = cargo_metadata::MetadataCommand::new()
        .manifest_path(cedarling_cargo_toml)
        .current_dir(&cwd)
        .exec()
        .unwrap();

    println!("cargo::rerun-if-changed={cedarling_cargo_toml}");
    println!("cargo::rerun-if-changed=build.rs");
    if let Some (root_pkg) = meta.root_package() {
        println!("cargo::warning=root pkg name {} version {}", root_pkg.name, root_pkg.version );
        // will be retrieved by env! macro in lib.rs
        println!("cargo::rustc-env=CEDARLING_VERSION={}", root_pkg.version);
    } else {
        println!("cargo::error=cedarling root package not found, check the location of Cargo.toml in this build.rs");
    }
}
