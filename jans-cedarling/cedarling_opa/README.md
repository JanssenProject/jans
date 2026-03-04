# OPA Cedarling Plugin

A policy evaluation plugin for [Open Policy Agent (OPA)](https://www.openpolicyagent.org/) that integrates with Cedarling.

## Building

To build an OPA binary that includes the plugin, you need the following:

- Go 1.25+
- Rust toolchain 1.56+
- Make (for building the plugin)

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

2. Create or edit the plugin configuration file (full example provided in [opa-config.json](./opa-config.json)

```json

{
    "decision_logs": {
        "plugin": "cedarling_opa"
    },
    "plugins": {
        "cedarling_opa": {
            "stderr": false,
            "bootstrap_config": {},
            "policy_store": {}
    }
}
```

Refer to the documentation for [bootstrap](https://docs.jans.io/stable/cedarling/reference/cedarling-properties/) and [policy store](https://docs.jans.io/stable/cedarling/reference/cedarling-policy-store/). 

3. Finally, run the binary with the plugin:

```
./build/opa-cedarling run --server --config-file opa-config.json
```
