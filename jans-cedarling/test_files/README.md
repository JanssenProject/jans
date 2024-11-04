# Folder with test policy store files

This folder contains policy store files used for unit testing purposes.

## Descriptions of json policy store test fixtures

These are required to have base64 encodings because they're used in testing decoding of base64

1. `policy-store_blobby.json`: fails because cedar-json appears to be unable to read policies in cedar-json format.
1. `policy-store_policy_err_base64.json`: contains broken base64 encoded policy.
1. `policy-store_policy_err_broken_utf8.json`: contains broken UTF8 policy base64 string.
1. `policy-store_readable.json`: Somewhat readable policies in json, as an example. Identical to `policy-store_readable.yaml`
1. `policy-store_schema_err_base64.json`: must be in json to test base64 decoding

## Descriptions of yaml policy store test fixtures

These are fully human-readable

1. `policy-store_ok.yaml`: The default "everything is fine" fixture - used in multiple tests.
1. `policy-store_ok_2.yaml`: Compared to previous - minimised fields in schema entities.
1. `policy-store_policy_err_broken_policy.yaml`: contains broken policy (should be error on compilation policy).
1. `policy-store_readable.yaml`: Similar to `policy-store_ok.yaml`, but no `Role` in schema. Identical to `policy-store_readable.json`.
1. `policy-store_schema_err_cedar_mistake.yaml`: contains broken cedar-policy schema (should be error on compilation).
1. `policy-store_schema_err.yaml`: contains broken json cedar-policy schema (should be error on reading json value).
1. `policy-store_with_trusted_issuers_ok.yaml`: list of trusted issues, seems to be newer current version of `policy-store_with_multiple_role_mappings_err.yaml`

## unused files
- policy-store_with_multiple_role_mappings_err.yaml

