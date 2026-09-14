# Folder with test policy store files

This folder contains policy store files used for unit testing purposes.

## Bootstrap config fixtures

- `bootstrap_props.json` / `bootstrap_props.yaml`: sample bootstrap config. `CEDARLING_POLICY_STORE_LOCAL_FN` points to `policy-store_ok.yaml` (Agama format with `policy_stores`). All keys match `BootstrapConfigRaw`.

## Descriptions of legacy json policy store test fixtures

These files are retained exclusively for negative test cases to verify that legacy JSON policy store loading is rejected with descriptive migration errors.

1. `policy-store_generated.json`: Legacy JSON store used to verify rejection when provided as a local file or inline.
1. `policy-store_lock_master_ok.json`: Legacy Lock Server JSON response payload used to verify rejection when retrieved via URI or refresh.

## Descriptions of yaml policy store test fixtures

YAML remains supported for test suites. These fixtures are used across unit and integration tests:

1. `policy-store_ok.yaml`: The default "everything is fine" fixture - used in multiple tests.
1. `policy-store_ok_2.yaml`: Simplified fixture for `authorize_unsigned` tests using `TestPrincipal1/2/3` entities.
1. `policy-store_no_trusted_issuers.yaml`: Like `policy-store_ok_2.yaml` but without trusted issuers (for unsigned-only tests).
1. `policy-store_policy_err_base64.yaml`: contains broken base64 encoded policy.
1. `policy-store_policy_err_broken_utf8.yaml`: contains broken UTF8 policy base64 string.
1. `policy-store_policy_err_broken_policy.yaml`: contains broken policy (should be error on compilation policy).
1. `policy-store_readable.yaml`: Similar to `policy-store_ok.yaml`, but no `Role` in schema.
1. `policy-store_schema_err_base64.yaml`: contains invalid base64 schema.
1. `policy-store_schema_err_cedar_mistake.yaml`: contains broken cedar-policy schema (should be error on compilation).
1. `policy-store_schema_err.yaml`: contains broken json cedar-policy schema (should be error on reading json value).
1. `policy-store_with_trusted_issuers_ok.yaml`: list of trusted issuers for multi-issuer tests.

