# Folder with test policy store files

This folder contains policy store files used for unit testing purposes.

## Descriptions of the policy store files

1. `policy-store_ok.json`: This file contains a valid policy store.
    - Human readable version of this file is available at the folder `policy-store_ok`.

1. `policy-store_policy_err_base64.json`: contains broken base64 encoded policy.
1. `policy-store_policy_err_broken_policy.json`: contains broken policy (should be error on compilation policy).
1. `policy-store_policy_err_broken_utf8.json`: contains broken UTF8 policy base64 string.

1. `policy-store_schema_err_base64.json`: contains broken base64 encoded schema.
1. `policy-store_schema_err_cedar_mistake.json`: contains broken cedar-policy schema (should be error on compilation).
1. `policy-store_schema_err_json.json`: contains broken json cedar-policy schema (should be error on reading json value).
