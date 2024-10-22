/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

// Save the current core cedarling version in an ENV var for use by the rust
// side of the ruby binding.
fn cedarling_version_in_env() {
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
    println!("cargo::rerun-if-changed=lib/cedarling_ruby");

    if let Some (root_pkg) = meta.root_package() {
        println!("cargo::rustc-env=CEDARLING_VERSION={}", root_pkg.version)
    } else {
        println!("cedarling root package not found, check the location of Cargo.toml in this build.rs");
        std::process::exit(1)
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
fn ruby_extension_link_options() -> Result<(),Box<dyn std::error::Error>> {
    let mut ruby_process = std::process::Command::new("ruby")
        .stdin(std::process::Stdio::piped()) // send script as stdin
        .stdout(std::process::Stdio::piped()) // Capture standard output
        .spawn()?;

    // This is effectively two lines of a ruby script, which output the relevant values.
    // Send script to the ruby process on its stdin
    use std::io::Write;
    if let Some(mut stdin) = ruby_process.stdin.take() {
        writeln!(stdin, "puts RbConfig::CONFIG['LIBS']")?; // fetch the -lm and -lpthread, or whatever is needed
        writeln!(stdin, "puts RbConfig::CONFIG['libdir']")?; // fetch the lib search path
    }

    // Now read the output from the ruby script
    let ruby_rbconfig_output = ruby_process.wait_with_output()?;

    if ruby_rbconfig_output.status.success() {
        use std::io::BufRead;
        let cursor = std::io::Cursor::new(ruby_rbconfig_output.stdout);
        let mut reader = std::io::BufReader::new(cursor);
        let mut line = String::new();

        // read the first output line, ie CONFIG['LIBS'] and retrieve the lib names.
        // This line should be something like "-lm -lpthread"
        // NOTE this might be linux-only, but it's for dev and won't be used for the main build.
        let _n = reader.read_line(&mut line)?;
        // output a separate cargo link command for each found ruby lib
        for lib_name in line.trim().replace("-l","").split(" ") {
            println!("cargo::rustc-link-lib={lib_name}");
        }

        // read second line and output it as the link-search
        // The line should be something like /usr/local/rvm/rubies/ruby-3.3.0/lib
        line.clear();
        let _n = reader.read_line(&mut line)?;
        let line = line.trim();
        // Yes, in fact we do not want std::fs::exists
        if !std::path::Path::new(line).exists() {
            return Err(format!("path {} does not exist", line).into())
        }
        // Tell the cargo linker where to find the ruby libs
        println!("cargo::rustc-link-search=native={line}");
    } else {
        println!("ruby rbconfig failed: {}", ruby_rbconfig_output.status);
        std::process::exit(1);
    };
    Ok(())
}

// This tells the linker to output the extension cdylib file to the correct
// directory where ruby can find it.
fn ruby_extension_dylib_path() -> Result<(),Box<dyn std::error::Error>> {
    // Apparently this is the only way to retrieve the platform appropriate
    // cdylib file extension. It's not so great.
    let cdylib_extension = match &std::env::var("CARGO_CFG_TARGET_OS")?[..] {
        "linux" => ".so",
        "windows" => ".dll",
        "macos" => ".dylib",
        other => return Err(format!("Attempting to calculate cdylib file extension, but unknown os encountered: {other}").into()),
    };

    // The easiest way to find out where to write the dylib file seems to be CARGO_MANIFEST_DIR.
    // Which points to the directory containing the Cargo.toml controlling this build.
    let manifest_dir = std::env::var("CARGO_MANIFEST_DIR")?;
    let manifest_dir = std::path::Path::new(&manifest_dir);
    let lib_dir = manifest_dir.join("../../lib/cedarling_ruby").canonicalize()?;

    // Ensure that lib/cedarling_ruby dir exists, otherwise linker fails.
    // TODO was this maybe also causing the trouble with the gem package and gem install?
    if !std::fs::exists(&lib_dir)? {
        std::fs::create_dir(&lib_dir).expect(&format!("cannot create {lib_dir:?}"));
    }

    // Finally, specify the output name of the file.
    let cedarling_ruby_dylib = lib_dir.join(format!("cedarling_ruby{cdylib_extension}"));

    // Tell the linker the dylib path to write.
    // NOTE the lib_ prefix of the file is absent, as required by ruby extension loading.
    // srsly not bothering to check utf-8 correctness of this.
    // DO NOT put a space after -o :-\
    println!("cargo:rustc-link-arg=-o{}", cedarling_ruby_dylib.to_str().expect("cannot convert cedarling_ruby_dylib to utf8"));

    Ok(())
}

fn main() {
    cedarling_version_in_env();
    ruby_extension_link_options().expect("failed to retrieve the relevant rbconfig from ruby");
    ruby_extension_dylib_path().expect("something went wrong creating the lib/cedarling_ruby/cedarling_ruby dylib or its path");
}
