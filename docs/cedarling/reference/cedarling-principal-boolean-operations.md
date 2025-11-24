---
tags:
  - administration
  - authorization / authz
  - Cedar
  - Cedarling
  - boolean
  - operations
---

# User-Workload Boolean Operation

The `CEDARLING_PRINCIPAL_BOOLEAN_OPERATION` property specifies what boolean operation to use when combining authorization decisions for `USER` and `WORKLOAD` principals. This JSON Logic rule determines the final authorization outcome based on individual principal decisions.

We use [JsonLogic](https://jsonlogic.com/) to define the boolean operation. The rule is evaluated against each principal decision, and the final result is determined based on the specified operation.

### Variables in the jsonlogic rule

Make sure that you use correct `var` name for `principal` types.

When referencing principals in your JSON logic rules, you must use the full Cedar principal type identifier that includes namespace, entity name and optionally the entity ID. This matches exactly how principals are defined in your Cedar policies.

**Correct Format**: `<Namespace>::<EntityType>` or `<Namespace>::<EntityType>::"<EntityID>"`  
Examples:  

* Without ID: `Jans::User`, `Jans::Workload`, `Acme::Service`
* With ID: `Jans::User::"john_doe"`, `Jans::Device::"mobile_1234"`, `Acme::Service::"api_gateway"`.  

*Notice*: Make sure to correctly escape `"` in JSON strings. For example `"Acme::Service::\"api_gateway\""`.

**Why This Matters**

* Matches Cedar's type system requirements
* Ensures proper variable resolution
* Maintains consistency with policy definitions

**Example Configuration**

```
// CORRECT - Full type with namespace
{
    "or": [
        {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
        {"===": [{"var": "Jans::User"}, "ALLOW"]}
    ]
}

// INCORRECT - Missing namespace
{
    "or": [
        {"===": [{"var": "Workload"}, "ALLOW"]}, // Will not resolve
        {"===": [{"var": "User"}, "ALLOW"]}      // Will not resolve
    ]
}
```

**Consequences of Incorrect Format**  
❌ Authorization will fail with DENY  
❌ Potential evaluation errors in JSON logic  
❌ Mismatches with actual Cedar policy definitions  

### Default configuration

Default value:

```json
{
    "and" : [
        {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
        {"===": [{"var": "Jans::User"}, "ALLOW"]}
    ]
}
```

Explanation:  

* The rule uses and to require both principals to be authorized
* `{"var": "Jans::Workload"}` checks the workload principal's decision
* `{"var": "Jans::User"}` checks the user principal's decision
* `"==="` performs strict equality comparison against "ALLOW"
* both conditions must be true for final authorization to be granted

### Comparison Operators

* === (Recommended): Strict equality check (type and value must match)
* ==: Loose equality check (may cause type coercion errors if variables are missing)

Note: For comparison better to use `===` instead of `==`. To avoid casting result to `Nan` if something goes wrong.

#### Operation Types

##### **AND Operation**

```js
{"and": [condition1, condition2]}
```

* Authorization succeeds only if ALL conditions are true

Example:

```json
{
    "and" : [
        {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
        {"===": [{"var": "Jans::User"}, "ALLOW"]}
    ]
}
```

##### **OR Operation**

```js
{"or": [condition1, condition2]}
```

* Authorization succeeds if ANY condition is true

Example:

```json
{
    "or" : [
        {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
        {"===": [{"var": "Jans::User"}, "ALLOW"]}
    ]
}
```

#### Best Practices

1. Use Strict Comparison (===)
   * Prevents unexpected type conversions
   * Returns DENY instead of errors when variables are missing
1. Explicit Principal References

    ```json
    // Good - explicit principal type
    {"===": [{"var": "Jans::Workload"}, "ALLOW"]}

    // Bad - incorrect principal type
    {"===": [{"var": "Workload"}, "ALLOW"]}
    ```

#### Error Scenarios

* Using == with missing principals:

```json
{"==": [{"var": "MissingPrincipal"}, "ALLOW"]}  // Throws error
```

* Type mismatches:

```json
{"===": [{"var": "Jans::Workload"}, true]}  // Always false
```
