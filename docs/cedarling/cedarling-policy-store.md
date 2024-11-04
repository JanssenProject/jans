---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - policy store
---

# Cedarling Policy Store

The Cedarling Policy Store uses a JSON file named `cedarling_store.json` to store all necessary data for evaluating policies and verifying JWT tokens. The structure includes the following key components:

1. **Cedar Schema**: The Cedar schema encoded in Base64.
2. **Cedar Policies**: The Cedar policies encoded in Base64.
3. **Trusted Issuers**: Details about the trusted issuers (see [below](#trusted-issuer-schema) for syntax).

**Note:** The `cedarling_store.json` file is only needed if the bootstrap properties: `CEDARLING_LOCK`; `CEDARLING_POLICY_STORE_URI`; and `CEDARLING_POLICY_STORE_ID` are not set to a local location. If you're fetching the policies remotely, you don't need a `cedarling_store.json` file.

### JSON Schema

The JSON Schema is for the `policy_store.json` defined as follows:

```json
{
    "cedar_version": "v2.4.7",
    "cedar_policies": { ... },
    "cedar_schema": "cedar_schema_encoded_in_base64",
    "trusted_issuers": [ ... ]
}

```

- **cedar_version** : (*String*) The version of [Cedar policy](https://docs.cedarpolicy.com/). The protocols of this version will be followed when processing Cedar schema and policies.
- **cedar_policies** : (*Object*) Object containing one or more policy IDs as keys, with their corresponding objects as values. See: [cedar_policies schema](#cedar-policies-schema).
- **cedar_schema** : (*String*) The Cedar schema, encoded in Base64 format.
- **trusted_issuers** : (*Array of [TrustedIssuer](#trusted-issuer-schema)*) List of metadata for Trusted Issuers.

## Cedar Policies Schema

The `cedar_policies` field outlines the Cedar policies that will be used in Cedarling. Multiple policies can be defined, with policy requiring a unique `policy_id`.

```json
  "cedar_policies": {
    "unique_policy_id": {
      "description": "simple policy example",
      "creation_date": "2024-09-20T17:22:39.996050",
      "policy_content": "cGVybWl0KAogICAgc..."
    },
    ...
  }
```

- **unique_policy_id**: (*String*) A uniqe policy ID used to for tracking and auditing purposes.
- **description** : (*String*) A brief description of cedar policy
- **creation_date** :  (*String*) Policy creating date in `YYYY-MM-DDTHH:MM:SS.ssssss`
- **policy_content** : (*String*) The policy content, encoded in Base64 format.

### Example of `cedar_policies`

Here is a non-normative example of the `cedar_policies` field:

```json
  "cedar_policies": {
    "840da5d85403f35ea76519ed1a18a33989f855bf1cf8": {
      "description": "simple policy example",
      "creation_date": "2024-09-20T17:22:39.996050",
      "policy_content": "cGVybWl0KAogICAgc..."
    },
    "0fo1kl928Afa0sc9123scma0123891asklajsh1233ab": {
      "description": "another policy example",
      "creation_date": "2024-09-20T18:22:39.192051",
      "policy_content": "kJW1bWl0KA0g3CAxa..."
    },
    ...
  }
```

## Trusted Issuer Schema

This record contains the information needed to validate tokens from this issuer:

```json
"trusted_issuers": [
    {
         "name": "name_of_the_trusted_issuer", 
         "description": "description_of_the_trusted_issuer", 
         "openid_configuration_endpoint": "https://<trusted-issuer-hostname>/.well-known/openid-configuration",
         "token_metadata": [ ... ]
    }
]
```

- **name** : (*String*) The name of the trusted issuer.
- **description** : (*String*) A brief description of the trusted issuer, providing context for administrators.
- **openid_configuration_endpoint** : (*String*) The HTTPS URL for the OpenID Connect configuration endpoint (usually found at `/.well-known/openid-configuration`).
- **token_metadata** : (*Array of [TokenMetadata](#token-metadata-schema)*, *optional*) Metadata related to the tokens issued by this issuer.

### Token Metadata Schema

```json
{
  "type": "access_token"
  "user_id": "some_user123",
  "role_mapping": "role",
}
```

- **type** : (String, no spaces) The type of token (e.g., Access, ID, Userinfo, Transaction).
- **user_id** : (String) The claim used to create the Cedar entity associated with this token.
- **role_mapping** : (String, *optional*) The claim used to create a role for the token. The default value of `role_mapping` is `role`.

**Note**: Only one token should include the `role_mapping` field in the list of `token_metadata`.

## Example Policy store

Here is a non-normative example of a `cedarling_store.json` file:

```json
{
    "cedar_version": "v2.4.7",
    "cedar_policies": {
        "840da5d85403f35ea76519ed1a18a33989f855bf1cf8": {
            "description": "simple policy example",
            "creation_date": "2024-09-20T17:22:39.996050",
            "policy_content": "cedar_policy_encoded_in_base64"
        }
    },
    "cedar_schema": "cedar_schema_encoded_in_base64",
    "trusted_issuers": [
        {
            "name": "Google",
            "description": "Consumer IDP",
            "openid_configuration_endpoint": "https://accounts.google.com/.well-known/openid-configuration",
            "token_metadata": [
                {
                    "type": "access_token",
                    "user_id": "aud"
                },
                {
                    "type": "id_token",
                    "user_id": "sub"
                },
                {
                    "type": "userinfo_token",
                    "user_id": "email",
                    "role_mapping": "role"
                }
            ]
        }
    ]
}
```

## Policy and Schema Authoring

You can hand create your Cedar policies and schema in
[Visual Studio](https://marketplace.visualstudio.com/items?itemName=cedar-policy.vscode-cedar).
Make sure you run the cedar command line tool to validate both your schema and policies.

The easiest way to author your policy store is to use the Policy Designer in
[Agama Lab](https://cloud.gluu.org/agama-lab). This tool helps you define the policies, schema and
trusted IDPs and to publish a policy store to a Github repository.
