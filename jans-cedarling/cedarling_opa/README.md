# OPA Cedarling Plugin

A policy evaluation plugin for [Open Policy Agent (OPA)](https://www.openpolicyagent.org/) that integrates with Cedarling, allowing users to perform Cedar-based authorization in OPA workflows.

## Building

To compile OPA with the Cedarling plugin, you need the following:

- Go 1.25+
- Rust toolchain 1.95+
- Make (for building the plugin. This build process is currently Linux only).

1. Clone the Janssen repository:

```
git clone --depth 1 https://github.com/JanssenProject/jans.git
```

2. Navigate to the `cedarling_opa` directory:

```
cd jans/jans-cedarling/cedarling_opa
```

3. Compile OPA and the plugin:

```
make
```
The Makefile compiles the OPA binary and the Rust binding library.

Output locations:

- Binary: `build/opa-cedarling`

- Library: `plugins/cedarling_opa/libcedarling_go.so`

To clean up build artifacts, run `make clean`

## Running

1. Set the library path so the plugin can find the Rust binding by running this from the `cedarling_opa` directory:

```
export LD_LIBRARY_PATH=$(pwd)/plugins/cedarling_opa:$LD_LIBRARY_PATH
```

2. Create or edit the plugin configuration file (full example provided in [opa-config.json](./demo/opa-config.json))

```json

{
    "plugins": {
        "cedarling_opa": {
            "stderr": false,
            "bootstrap_config": {}
        }
    }
}
```
- `stderr`: Whether or not the **plugin** emits errors to stdout or stderr
- `bootstrap_config`: Bootstrap configuration dictionary for the Cedarling instance. Refer to the documentation for [bootstrap](https://docs.jans.io/stable/cedarling/reference/cedarling-properties/) and [policy store](https://docs.jans.io/stable/cedarling/reference/cedarling-policy-store/) configuration. 

3. Finally, run the binary with the plugin and provided rego examples:

```bash
./build/opa-cedarling run --server --config-file ./demo/opa-config.json ./demo/rego
```
OPA will boot with the provided configuration, read the rego files, and start server mode at `127.0.0.1:8181`.

## Docker
A Dockerfile is provided to allow building a docker image embedded with the bootstrap configuration and rego files. To build and run this image:

- Edit `demo/opa-config.json` to your specification
- Place your rego files in `demo/rego`
- Build:
```
docker build . -t opa-cedarling:latest
```
- And run:
```
docker run -p 8181:8181 opa-cedarling:latest
```

## Demo
The `demo` folder provides a set of defaults to demonstrate the plugin. The configuration file contains a bootstrap for Cedarling where the policy store is configured for unsigned authorization. The `rego` folder contains two example Rego files, one for unsigned and one for multi issuer authorization. Since the policy store does not contain any trusted issuers, multi-issuer authorization is not available with this policy store.
