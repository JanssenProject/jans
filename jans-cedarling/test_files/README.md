# Folder with test policy store files

This folder contains policy store files used for unit testing purposes.

## Descriptions of json policy store test fixtures

These are required to have base64 encodings becaseu they're used in testing decoding of base64

1. `policy-store_policy_err_base64.json`: contains broken base64 encoded policy.
1. `policy-store_policy_err_broken_utf8.json`: contains broken UTF8 policy base64 string.

policy-store_blobby.json
policy-store_ok.json
policy-store_readable.json
policy-store_schema_err_base64.json

1. `policy-store_ok.json`: This file contains a valid policy store.
    - Human readable version of this file is available at the folder `policy-store_ok`.

## Descriptions of yaml policy store test fixtures

These are fully human-readable

1. `policy-store_policy_err_broken_policy.yaml`: contains broken policy (should be error on compilation policy).
1. `policy-store_schema_err_cedar_mistake.yaml`: contains broken cedar-policy schema (should be error on compilation).
1. `policy-store_schema_err_json.yaml`: contains broken json cedar-policy schema (should be error on reading json value).

## unused files
- policy-store_with_multiple_role_mappings_err.yaml

