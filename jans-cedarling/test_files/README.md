# Folder with test policy store files

This folder contains policy store files used for unit testing purposes.

## Bootstrap config fixtures

- `bootstrap_props.json` / `bootstrap_props.yaml`: sample bootstrap config. `CEDARLING_POLICY_STORE_LOCAL_FN` points to `policy-store_ok.yaml` (Agama format with `policy_stores`). All keys match `BootstrapConfigRaw`; deprecated options (e.g. `CEDARLING_USER_AUTHZ`) are not present.

## Descriptions of json policy store test fixtures

These are required to have base64 encodings because they're used in testing decoding of base64

1. `policy-store_blobby.json`: flat format (top-level `cedar_policies`/`cedar_schema`), not Agama format. Used in unit tests that expect deserialization to fail. Do not use as `CEDARLING_POLICY_STORE_LOCAL_FN`; use `policy-store_ok.yaml` or another file with `policy_stores` instead.
1. `policy-store_policy_err_base64.json`: contains broken base64 encoded policy.
1. `policy-store_policy_err_broken_utf8.json`: contains broken UTF8 policy base64 string.
1. `policy-store_generated.json`: Agama format with `policy_stores`; uses `trusted_issuers` and `token_metadata` (not `identity_source`/`access_tokens`) to match `PolicyStore`/`TrustedIssuer` in code.
1. `policy-store_readable.json`: Somewhat readable policies in json, as an example. Identical to `policy-store_readable.yaml`
1. `policy-store_schema_err_base64.json`: must be in json to test base64 decoding

## Descriptions of yaml policy store test fixtures

These are fully human-readable

1. `policy-store_ok.yaml`: The default "everything is fine" fixture - used in multiple tests.
1. `policy-store_ok_2.yaml`: Simplified fixture for `authorize_unsigned` tests using `TestPrincipal1/2/3` entities.
1. `policy-store_no_trusted_issuers.yaml`: Like `policy-store_ok_2.yaml` but without trusted issuers (for unsigned-only tests).
1. `policy-store_policy_err_broken_policy.yaml`: contains broken policy (should be error on compilation policy).
1. `policy-store_readable.yaml`: Similar to `policy-store_ok.yaml`, but no `Role` in schema. Identical to `policy-store_readable.json`.
1. `policy-store_schema_err_cedar_mistake.yaml`: contains broken cedar-policy schema (should be error on compilation).
1. `policy-store_schema_err.yaml`: contains broken json cedar-policy schema (should be error on reading json value).
1. `policy-store_with_trusted_issuers_ok.yaml`: list of trusted issuers for multi-issuer tests.

