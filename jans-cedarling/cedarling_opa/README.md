# OPA Cedarling Plugin

A policy evaluation plugin for [Open Policy Agent (OPA)](https://www.openpolicyagent.org/) that integrates with Cedarling.

## Building

To build an OPA binary that includes the plugin, you need the following:

- Go 1.25+
- Rust toolchain 1.56+
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

## Running

1. Set the library path so the plugin can find the Rust binding:

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

Refer to the documentation for [bootstrap](https://docs.jans.io/stable/cedarling/reference/cedarling-properties/) and [policy store](https://docs.jans.io/stable/cedarling/reference/cedarling-policy-store/) configuration. 

3. Finally, run the binary with the plugin and provided rego examples:

```
./build/opa-cedarling run --server --config-file ./demo/opa-config.json ./demo/rego
```
## Docker
