/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

fn manifest_dir() -> std::path::PathBuf {
    let manifest_dir = std::env::var("CARGO_MANIFEST_DIR").expect("CARGO_MANIFEST_DIR not in build script. Wut!?");
    std::path::Path::new(&manifest_dir).to_path_buf()
}

// Save the current core cedarling version in an ENV var for use by the rust
// side of the ruby binding.
fn cedarling_version_in_env() {
    // Fetch cedarling version from its Cargo.toml
    //
    // This will panic if cwd contains non-utf8 characters. But it's a build
    // script, so we can assume that a failure here is not catastrophic.
    let cedarling_cargo_toml = manifest_dir().join("../../../../cedarling/Cargo.toml").canonicalize().unwrap();
    let meta = cargo_metadata::MetadataCommand::new()
        .manifest_path(&cedarling_cargo_toml)
        .current_dir(manifest_dir())
        .exec()
        .unwrap();

    println!("cargo::rerun-if-changed={}", cedarling_cargo_toml.to_str().unwrap());
    println!("cargo::rerun-if-changed=build.rs");
    println!("cargo::rerun-if-changed=lib/cedarling_ruby");

    if let Some (root_pkg) = meta.root_package() {
        // will be retrieved by env! macro in lib.rs
        println!("cargo::rustc-env=CEDARLING_VERSION={}", root_pkg.version)
    } else {
        panic!("cedarling root package not found, check the location of Cargo.toml in this build.rs")
    }
}

// The officially recommended way to build a ruby extension in rust is to execute
//
//   rake compile:dev
//
// However, that rebuilds the whole of the cedarling codebase and all of its
// dependency crates, from scratch every time. While that is suitable for a
// single build of the gem during installation, clearly it is unsuitable for
// normal development, where frequent recompiles of small parts of the codebase
// are typical.
//
// The final step of the `rake compile:dev` process links the output dynamic
// library to some posix dependencies, without which ruby will not load the
// extension. Something like:
//
//   -C linker=gcc -L native=/usr/local/rvm/rubies/ruby-3.3.0/lib -C link-arg=-lm -l pthread
//
// This function outputs build commands which performs that linking, but without
// requiring a recompile of the entire codebase and all of its dependency
// crates.
fn ruby_extension_link_options() {
    let mut ruby_process = std::process::Command::new("ruby")
        .stdin(std::process::Stdio::piped()) // send script as stdin
        .stdout(std::process::Stdio::piped()) // Capture standard output
        .spawn()
        .unwrap();

    // This is effectively two lines of a ruby script, which output the relevant values.
    // Send script to the ruby process on its stdin
    use std::io::Write;
    if let Some(mut stdin) = ruby_process.stdin.take() {
        writeln!(stdin, "puts RbConfig::CONFIG['LIBS']").unwrap(); // fetch the -lm and -lpthread, or whatever is needed
        writeln!(stdin, "puts RbConfig::CONFIG['libdir']").unwrap(); // fetch the lib search path
    }

    // Now read the output from the ruby script
    let ruby_rbconfig_output = ruby_process.wait_with_output().unwrap();

    if ruby_rbconfig_output.status.success() {
        use std::io::BufRead;
        let cursor = std::io::Cursor::new(ruby_rbconfig_output.stdout);
        let mut reader = std::io::BufReader::new(cursor);
        let mut line = String::new();

        // read the first output line, ie CONFIG['LIBS'] and retrieve the lib names.
        // This line should be something like "-lm -lpthread"
        // NOTE this might be linux-only, but it's for dev and won't be used for the main build.
        let _n = reader.read_line(&mut line).unwrap();
        // output a separate cargo link command for each found ruby lib
        for lib_name in line.trim().replace("-l","").split(" ") {
            println!("cargo::rustc-link-lib={lib_name}");
        }

        // read second line and output it as the link-search
        // The line should be something like /usr/local/rvm/rubies/ruby-3.3.0/lib
        line.clear();
        let _n = reader.read_line(&mut line).unwrap();
        let line = line.trim();
        // Yes, in fact we do not want std::fs::exists
        if !std::path::Path::new(line).exists() {
            panic!("path {} does not exist", line)
        }
        // Tell the cargo linker where to find the ruby libs
        println!("cargo::rustc-link-search=native={line}");
    } else {
        panic!("ruby rbconfig failed: {}", ruby_rbconfig_output.status)
    };
}

// This tells the linker to output the extension cdylib file to the correct
// directory where ruby can find it.
fn ruby_extension_dylib_path() {
    // Apparently this is the only way to retrieve the platform appropriate
    // cdylib file extension. It's not so great.
    let cdylib_extension = match &std::env::var("CARGO_CFG_TARGET_OS").unwrap()[..] {
        "linux" => ".so",
        "windows" => ".dll",
        "macos" => ".dylib",
        other => panic!("Attempting to calculate cdylib file extension, but unknown CARGO_CFG_TARGET_OS encountered: {other}"),
    };

    // The easiest way to find out where to write the dylib file seems to be CARGO_MANIFEST_DIR.
    // Which points to the directory containing the Cargo.toml controlling this build.
    let lib_dir = manifest_dir().join("../../lib/cedarling_ruby");

    // Ensure that lib/cedarling_ruby dir exists, otherwise linker fails.
    // TODO was this maybe also causing the trouble with the gem package and gem install?
    if !std::fs::exists(&lib_dir).unwrap() {
        std::fs::create_dir(&lib_dir)
            .unwrap_or_else(|err| panic!("cannot create directory {lib_dir:?} {err}"));
    }
    let lib_dir = lib_dir.canonicalize().unwrap();

    // Finally, specify the output name of the file.
    let cedarling_ruby_dylib = lib_dir.join(format!("cedarling_ruby{cdylib_extension}"));

    // Tell the linker the dylib path to write.
    // NOTE the lib_ prefix of the file is absent, as required by ruby extension loading.
    // srsly not bothering to check utf-8 correctness of this.
    // DO NOT put a space after -o :-\
    println!("cargo:rustc-link-arg=-o{}", cedarling_ruby_dylib.to_str().expect("cannot convert cedarling_ruby_dylib to utf8"));
}

fn main() {
    cedarling_version_in_env();
    ruby_extension_link_options();
    ruby_extension_dylib_path();
}
