---
tags:
  - Cedar
  - Cedarling
  - OPA
---

# Cedarling OPA Plugin
A policy evaluation plugin for [Open Policy Agent (OPA)](https://www.openpolicyagent.org/) that integrates with Cedarling, allowing users to perform Cedar-based authorization in OPA workflows.

## Functionality

1. The OPA binary reads rego files and a configuration file containing bootstrap properties and initializes a Cedarling instance accordingly. Then the binary starts in server mode.
1. The user may then send queries to the OPA server over HTTP. OPA will call the Cedarling instance's authorization methods and return responses.

```mermaid
sequenceDiagram
    participant Client
    participant OPA
    participant Rego
    participant Builtin as cedarling.opa.authorize_multi_issuer
    participant Plugin as Cedarling Binding (Go)
    participant Cedarling
    participant Cedar

    Client->>OPA: Query (input)
    OPA->>Rego: Evaluate rego policy

    Rego->>Builtin: cedarling.opa.authorize_multi_issuer(input)

    Builtin->>Plugin: Invoke Go built-in
    Plugin->>Cedarling: Build + send request
    Cedarling->>Cedar: Evaluate cedar policies

    Cedar-->>Cedarling: Decision + diagnostics
    Cedarling-->>Plugin: Result
    Plugin-->>Builtin: Normalized JSON

    Builtin-->>Rego: result
    Rego-->>OPA: decision object

    OPA-->>Client: Response
```

## Rego functions

The plugin provides two new Rego functions:

- `cedarling.opa.authorize_multi_issuer(input)`: Calls the [multi-issuer authorization](../reference/cedarling-authz.md#multi-issuer-authorization-authorize_multi_issuer-recommended) interface.
    ```json title="OPA Query Payload"
    {
      "input": {
        "tokens": [
          {
            "mapping": "Jans::Access_token",
            "payload": "<base64url-encoded JWT>"
          },
          {
            "mapping": "Jans::id_token",
            "payload": "<base64url-encoded JWT>"
          }
        ],
        "action": "Jans::Action::\"Read\"",
        "resource": {
          "cedar_entity_mapping": {
            "entity_type": "Jans::SecretDocument",
            "id": "f865a1c0b8f37b0b5506be23de923d60"
          }
        },
        "context": {
          "network": "127.0.0.1",
          "current_time": 1776826458
        }
      }
    }
    ```
- `cedarling.opa.authorize_unsigned(input)`: Calls the [unsigned authorization](../reference/cedarling-authz.md#unsigned-authorization-authorize_unsigned) interface.
    ```json title="OPA Query Payload"
    {
      "input": {
        "principal": {
          "cedar_entity_mapping": {
            "entity_type": "Jans::User",
            "id": "2773c228886ad3c1202d4e5b59bc74dc"
          },
          "sub": "68971523-c8bd-474c-a712-c7dc7e41296a",
          "role": [
            "Teacher"
          ]
        },
        "action": "Jans::Action::\"Read\"",
        "resource": {
          "cedar_entity_mapping": {
            "entity_type": "Jans::SecretDocument",
            "id": "1d096225ac65fc42dff462f910df1eee"
          }
        },
        "context": {
          "network": "127.0.0.1",
          "current_time": 1776826458
        }
      }
    }
    ```
The result from these functions will be in the following format:
```json title="Output schema"
{
  "decision": true,
  "reasons": ["policy-1"],
  "errors": [],
  "request_id": "a1484f38-253f-41a2-8f54-5c0d07b62784"
}
```
and can be stored in a variable to perform Rego operations on.

### Response schema
The response from the `cedarling.opa.*` functions contain the following fields:

- **decision**: `true` if overall authorization decision from Cedarling is `allow`, otherwise `false`.
- **reasons**: array of strings containing policy ID(s) that resulted in the decision.
- **errors**: array of strings containing zero or more errors during authorization. This field being populated will result in decision being `false`.
- **request_id**: ID of the authorization request performed.

### Example Rego policy
```rego
package cedarling_opa

default allow := false

result := cedarling.opa.authorize_multi_issuer(input)

allow if {
	result.decision == true
}

deny_reasons := result.reasons
```

## Building

!!! note
    The FFI binding and OPA plugin can be built on Windows, macOS and Linux, but building on Linux is recommended and documented below. To build on Windows or macOS, simply follow the build instructions without Makefile and replace the binding library name as such:

    - Windows: `cedarling_go.dll`, `cedarling_go.dll.lib`
    - macOS: `libcedarling_go.dylib`
    
    For instructions on dynamic linking on Windows and macOS, please refer to the binding [documentation](https://github.com/JanssenProject/jans/tree/main/jans-cedarling/bindings/cedarling_go#build-your-go-application-with-dynamic-linking).

Required:

- Go 1.25+
- Rust toolchain 1.95+

Optional:

- Make (used to simplify the build process via provided Makefile)

**Build steps**:

1. Clone the `jans-cedarling` folder of the Janssen Repository:
    ```bash
    git clone --filter blob:none --no-checkout https://github.com/JanssenProject/jans
    cd jans
    git sparse-checkout init --cone
    git checkout main
    git sparse-checkout set jans-cedarling
    cd jans-cedarling/cedarling_opa
    ```
1. Build the plugin (if using Make)
    ```bash
    make
    ```
    The Makefile will build the Rust and Go artifacts and place them in the appropriate folders. To clean up, run `make clean`
1. OR run build steps manually:
    ```bash
    cargo build --release -p cedarling_go
    cp ../target/release/libcedarling_go.so plugins/cedarling_opa/
    mkdir -p build
    export CGO_ENABLED=1
    export CGO_LDFLAGS="-Lplugins/cedarling_opa"
    go build -o build/opa-cedarling
    ```

## Running

1. Set the library path so the plugin can find the Rust binding by running this from the `cedarling_opa` directory:

    ```bash
    export LD_LIBRARY_PATH=$(pwd)/plugins/cedarling_opa:$LD_LIBRARY_PATH
    ```

1. Create or edit the plugin configuration file `demo/opa-config.json`

    ```json title="OPA Config"
    
    {
        "plugins": {
            "cedarling_opa": {
                "stderr": false,
                "bootstrap_config": {} // fill with values
            }
        }
    }
    ```

    - `stderr`: Whether or not the **plugin** emits errors to stdout or stderr
    - `bootstrap_config`: Bootstrap configuration dictionary for the Cedarling instance. Refer to the documentation for [bootstrap](../reference/cedarling-properties.md) and [policy store](../reference/cedarling-policy-store.md) configuration.

1. Finally, run the binary with the plugin and provided rego examples:
    ```bash
    ./build/opa-cedarling run --server --config-file ./demo/opa-config.json ./demo/rego
    ```
OPA will boot with the provided configuration, read the rego files, and start server mode at `127.0.0.1:8181`.

## Querying

To interact with the OPA server, we can send queries for specific rules with the input. Let us assume we're using this Rego policy:

```rego
package cedarling_opa

default allow := false

result := cedarling.opa.authorize_multi_issuer(input)

allow if {
	result.decision == true
}

deny_reasons := result.reasons
```

alongside this cedar policy configured in a policy store:
```cedar
@id("allow_student_read")
permit (
  principal,
  action in [Jans::Action::"Read"],
  resource
)
when {
  context has tokens.jans_userinfo_token &&
  context.tokens.jans_userinfo_token.hasTag("role") &&
  context.tokens.jans_userinfo_token.getTag("role").contains("Student")
};
```
Multi-issuer authorization places the fields of the token payloads in the context, which is how we access those fields in the policy.

To perform a Rego query we can send:
```bash
$ curl -X POST http://localhost:8181/v1/data/cedarling_opa/result \
    -H "Content-Type: application/json" \
    -d '{
      "input": {
        "tokens": [
            {
                "mapping": "Jans::Userinfo_token",
                "payload":"<base64url-encoded JWT>"
            }
        ],
        "action": "Jans::Action::\"Read\"",
        "resource": {
          "cedar_entity_mapping": {
            "entity_type": "Jans::SecretDocument",
            "id": "bcd6e035ce2aabad2db27fb963facd41"
          }
        },
        "context": {
          "network": "127.0.0.1",
          "current_time": 1776826458
        }
      }
    }'
```
Where `<base64url-encoded JWT>` contains the following payload:
```json title="JWT Payload"
{
  "sub": "98iLfSWKxF_E1xGeu3sULkk0_y6xIwBP5b3OGUV33S0",
  "aud": "60dc2b2a-dc74-4b4c-bd9e-33d6ae95dae1",
  "nbf": 1776740629,
  "role": [
    "Student"
  ],
  "iss": "https://test.jans.org",
  "exp": 1776744229,
  "iat": 1776740629,
  "jti": "cq7B4lTgQM2ITbK65mkM0A",
  "client_id": "60dc2b2a-dc74-4b4c-bd9e-33d6ae95dae1"
}
```
And we get a response:
```json title="Response"
{
  "result": {
    "decision": true,
    "errors": [],
    "reasons": [
      "allow_student_read"
    ],
    "request_id": "019dadff-2481-7d0d-b3fc-cce7e05ef2c0"
  }
}
```

## Docker
A Dockerfile is provided to allow building a docker image embedded with the bootstrap configuration and rego files. To build and run this image:

- Edit `demo/opa-config.json` to your specification
- Place your rego files in `demo/rego`
- Build:
```bash
docker build . -t opa-cedarling:latest
```
- And run:
```bash
docker run -p 8181:8181 opa-cedarling:latest
```
