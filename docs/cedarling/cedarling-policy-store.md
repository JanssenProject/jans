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

**Note:** The `cedarling_store.json` file is only needed if the bootstrap property, `CEDARLING_POLICY_STORE_URI`, is not set to a local location. If it is a non-local location, Cedarling will fetch the policy store from the URI.

### JSON Schema Example

Here is a non-normative example of a `cedarling_store.json` file:

```json
{
    "cedar_version": "v2.4.7",
    "cedar_policies": {
        "unique_policy_id": {
            "description": "simple policy example",
            "creation_date": "2024-09-20T17:22:39.996050",
            "policy_content": "cedarling_policy_encoded_in_base64"
        }
    },
    "cedar_schema": "cedar_schema_encoded_in_base64",
    "trusted_issuers": [
        {
            "name": "IDP1",
            "description": "some idp",
            "openid_configuration_endpoint": "https://www.idp.com/.well-known/openid-configuration",
            "token_metadata": [
                {
                    "type": "Access",
                    "person_id": "aud"
                },
                {
                    "type": "Id",
                    "person_id": "sub"
                },
                {
                    "type": "userinfo",
                    "person_id": "email",
                    "role_mapping": "role"
                }
            ]
        }
    ]
}
```

## Trusted Issuer Schema

- **name** : (String, no spaces) The name of the trusted issuer.
- **description** : (String) A brief description, providing context for administrators.
- **openid_configuration_endpoint** : (String) The HTTPS URL for the OpenID Connect configuration endpoint (usually found at `/.well-known/openid-configuration`).
- **token_metadata** : (List[TokenMetadata](#token-metadata-schema), *optional*) Metadata related to the tokens issued by this issuer.

### Token Metadata Schema

- **type** : (String, no spaces) The type of token (e.g., Access, ID, Userinfo, Transaction).
- **person_id** : (String) The claim used to create the person entity associated with this token.
- **role_mapping** : (String, *optional*) The claim used to create a role for the token.

**Note**: Only one token should include the `role_mapping` field in the list of `token_metadata`.

## Policy and Schema Authoring

You can hand create your Cedar policies and schema in 
[Visual Studio](https://marketplace.visualstudio.com/items?itemName=cedar-policy.vscode-cedar). 
Make sure you run the cedar command line tool to validate both your schema and policies. 

The easiest way to author your policy store is to use the Policy Designer in 
[Agama Lab](https://cloud.gluu.org/agama-lab). This tool helps you define the policies, schema and
trusted IDPs and to publish a policy store to a Github repository.
