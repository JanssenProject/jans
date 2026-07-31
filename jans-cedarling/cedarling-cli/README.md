# cedarling-cli

A local test runner and debug utility for Cedarling policy stores.

This tool allows policy authors to test Cedarling policies locally without writing a full application (closing the author → test → deploy loop). Unlike `cedar run-tests`, which tests pure Cedar policies against raw Cedar entities, this tool tests the Cedarling-specific layer: policy store YAML/JSON format, `EntityData`, and bootstrap configuration.

## Installation

You can install the CLI from the `jans-cedarling` workspace using cargo:

```bash
cargo install --path ./cedarling-cli
```

Alternatively, you can run it directly from the workspace root:

```bash
cargo run -p cedarling-cli -- <subcommand> [args...]
```

## Subcommands

The CLI exposes three subcommands:

1. `test`: Run a suite of tests from a YAML file against the configured policy store.
2. `authorize`: Evaluate a single authorization request against the configured policy store.
3. `validate`: Validates a policy store across three independent checks: parse, schema, and metadata.

## Configuration and Env-var Precedence

The CLI uses the exact same `BootstrapConfig` logic as the Cedarling library. The configuration is layered with the following precedence (**flags override env override file**):

1. **File**: `--config <path>` (or `CEDARLING_CONFIG`)
2. **Environment Variables**: e.g., `CEDARLING_POLICY_STORE_URI`
3. **CLI Flags**: `--policy-store <path>`, `--log-type memory|stdout|off`, `--log-level <level>`, etc.

## `test` Subcommand

The `test` subcommand reads a YAML file describing one or more test cases and evaluates them. 
The YAML model closely mirrors the Cedarling on-wire shape, but uses a flattened structure to make test writing intuitive.

### YAML Test Format Example

```yaml
tests:
  - name: "Allow Alice to read her own document"
    request:
      principal:
        type: "Jans::User"
        id: "alice123"
        attributes:
          email: "alice@example.com"
          role: "User"
      action: "Jans::Action::\"Read\""
      resource:
        type: "Jans::Document"
        id: "doc-alice"
        attributes:
          owner: "alice123"
      context:
        network: "internal"
    result:
      decision: Allow
      reason_ids:
        - "policy_user_read_own_document"

  - name: "Deny Alice reading Bob's document"
    request:
      principal:
        type: "Jans::User"
        id: "alice123"
      action: "Jans::Action::\"Read\""
      resource:
        type: "Jans::Document"
        id: "doc-bob"
        attributes:
          owner: "bob456"
      context: {}
    result:
      decision: Deny
      num_errors: 0
```

### Coverage Report

The `test` subcommand tracks which policies were triggered across all tests in the suite. At the end of the test run, it computes a coverage report showing triggered vs untriggered policies.

_Note: Untriggered policies do not cause the test run to fail._

## `validate` Subcommand

The `validate` subcommand parses a policy store and executes three independent levels of validation:
1. **Parse**: Verifies Cedar syntax and parses JSON/YAML structures.
2. **Schema**: Verifies that all policies semantically match the schema (e.g. no undefined entity types or undefined attributes without `has` checks).
3. **Metadata**: Verifies Cedarling-specific metadata (e.g. `cedar_version`, timestamp ordering, naming).

It prints structured pass/fail per level with file/line/column where available.

### Passing Example
```
validating tests/test_store.yaml
  parse    ................ ok
  schema   ................ ok
  metadata ................ ok
validation passed
```

### Failing Example
```
validating tests/test_store_broken.yaml
  parse    ................ ok
  schema   ................ FAIL
    policy_123: for policy `policy_123`, unable to guarantee safety of access to optional attribute `id_token` on entity type `Jans::User`
  metadata ................ ok
validation failed: 1 error(s)
```

## Exit Codes

| Code | Meaning |
|---|---|
| `0` | Success (all tests passed, allow decision, or validation passed). |
| `1` | Test failure, deny decision, or validation report failures. |
| `2` | Configuration error, missing policy store, infra failure, or initialization error. |
