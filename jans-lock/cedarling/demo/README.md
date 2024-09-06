# Demo data folder

This folder contains the necessary data to run the example Rust crate **`authz_run`**.

## Files

- **`policy-store/local.json`**  
  This file represents the [policy store](https://docs.jans.io/head/admin/lock/#policy-store), containing policy information required to initialize the authorization process. It stores the policies that will be evaluated during authorization checks.

- **`policies_1.cedar`**  
  This file contains a decoded Cedar policy, which corresponds to the policy data in the **policy store**. It describes the authorization rules in Cedar format.

- **`input.json`**  
  This file contains the input data, formatted in JSON, that can be used to make authorization requests. It defines the specific authorization scenarios to be tested.
