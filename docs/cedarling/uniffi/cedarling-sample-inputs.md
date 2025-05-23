## bootstrap.json

```declarative
{
  "CEDARLING_APPLICATION_NAME": "My App",
  "CEDARLING_AUDIT_HEALTH_INTERVAL": 0,
  "CEDARLING_AUDIT_TELEMETRY_INTERVAL": 0,
  "CEDARLING_DYNAMIC_CONFIGURATION": "disabled",
  "CEDARLING_ID_TOKEN_TRUST_MODE": "strict",
  "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED": [
    "HS256",
    "RS256"
  ],
  "CEDARLING_JWT_SIG_VALIDATION": "disabled",
  "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
  "CEDARLING_LISTEN_SSE": "disabled",
  "CEDARLING_LOCAL_JWKS": null,
  "CEDARLING_LOCAL_POLICY_STORE": null,
  "CEDARLING_LOCK": "disabled",
  "CEDARLING_LOCK_MASTER_CONFIGURATION_URI": null,
  "CEDARLING_LOCK_SSA_JWT": null,
  "CEDARLING_LOG_LEVEL": "DEBUG",
  "CEDARLING_LOG_TTL": 120,
  "CEDARLING_LOG_TYPE": "memory",
  "CEDARLING_POLICY_STORE_ID": "840da5d85403f35ea76519ed1a18a33989f855bf1cf8",
  "CEDARLING_POLICY_STORE_LOCAL_FN": "./custom/static/policy-store.json",
  "CEDARLING_POLICY_STORE_URI": "",
  "CEDARLING_USER_AUTHZ": "enabled",
  "CEDARLING_PRINCIPAL_BOOLEAN_OPERATION": {
    "or": [
      {
        "and": [
          {
            "===": [
              {
                "var": "Jans::Workload"
              },
              "ALLOW"
            ]
          },
          {
            "===": [
              {
                "var": "Jans::User"
              },
              "ALLOW"
            ]
          }
        ]
      },
      {
        "and": [
          {
            "===": [
              {
                "var": "Jans::TestPrincipal1"
              },
              "ALLOW"
            ]
          },
          {
            "===": [
              {
                "var": "Jans::TestPrincipal2"
              },
              "ALLOW"
            ]
          }
        ]
      }
    ]
  },
  "CEDARLING_WORKLOAD_AUTHZ": "enabled",
  "id": "67d412fb-5dd9-4f85-9bd3-7b6471d90aa3"
}
```

## policy-store.json

```declarative
{
  "cedar_version": "v4.0.0",
  "policy_stores": {
    "a1bf93115de86de760ee0bea1d529b521489e5a11747": {
      "cedar_version": "v4.0.0",
      "name": "Jans",
      "description": "A test policy store where everything is fine.",
      "trusted_issuers": {
        "some_test_iss_id": {
          "name": "TestIss",
          "description": "Some Test Issuer",
          "openid_configuration_endpoint": "https://account.gluu.org/.well-known/openid-configuration",
          "token_metadata": {
            "access_token": {
              "entity_type_name": "Jans::Access_token",
              "workload_id": "client_id",
              "principal_mapping": [
                "Jans::Workload"
              ]
            },
            "id_token": {
              "entity_type_name": "Jans::Id_token",
              "user_id": "sub",
              "principal_mapping": [
                "Jans::User"
              ]
            },
            "userinfo_token": {
              "entity_type_name": "Jans::Userinfo_token",
              "user_id": "sub",
              "principal_mapping": [
                "Jans::User"
              ]
            }
          }
        }
      },
      "policies": {
        "840da5d85403f35ea76519ed1a18a33989f855bf1cf8": {
          "description": "simple policy example for principal workload",
          "creation_date": "2024-09-20T17:22:39.996050",
          "policy_content": {
            "encoding": "none",
            "content_type": "cedar",
            "body": "permit(\n    principal is Jans::Workload,\n    action in [Jans::Action::\"Update\"],\n    resource is Jans::Issue\n)when{\n    principal.sub == resource.sub\n};"
          }
        },
        "444da5d85403f35ea76519ed1a18a33989f855bf1cf8": {
          "cedar_version": "v4.0.0",
          "description": "simple policy example for principal user",
          "creation_date": "2024-09-20T17:22:39.996050",
          "policy_content": {
            "encoding": "none",
            "content_type": "cedar",
            "body": "permit(\n    principal is Jans::User,\n    action in [Jans::Action::\"Update\"],\n    resource is Jans::Issue\n)when{\n    principal.sub == resource.sub\n};"
          }
        },
        "TestPrincipal1": {
          "cedar_version": "v4.0.0",
          "description": "simple policy example for TestPrincipal1",
          "creation_date": "2024-09-20T17:22:39.996050",
          "policy_content": {
            "encoding": "none",
            "content_type": "cedar",
            "body": "permit(\n    principal is Jans::TestPrincipal1,\n    action,\n    resource\n)when{\n principal.is_ok\n};"
          }
        },
        "TestPrincipal2": {
          "cedar_version": "v4.0.0",
          "description": "simple policy example for TestPrincipal2",
          "creation_date": "2024-09-20T17:22:39.996050",
          "policy_content": {
            "encoding": "none",
            "content_type": "cedar",
            "body": "permit(\n    principal is Jans::TestPrincipal2,\n    action,\n    resource\n)when{\n principal.is_ok\n};"
          }
        }
      },
      "schema": "ewoiSmFucyI6IHsKImNvbW1vblR5cGVzIjogewoiQ29udGV4dCI6IHsKInR5cGUiOiAiUmVjb3JkIiwKImF0dHJpYnV0ZXMiOiB7CiJhY2Nlc3NfdG9rZW4iOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiQWNjZXNzX3Rva2VuIiwKInJlcXVpcmVkIjogZmFsc2UKfSwKImlkX3Rva2VuIjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIklkX3Rva2VuIiwKInJlcXVpcmVkIjogZmFsc2UKfSwKInVzZXIiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiVXNlciIsCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJ1c2VyaW5mb190b2tlbiI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJVc2VyaW5mb190b2tlbiIsCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJ3b3JrbG9hZCI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJXb3JrbG9hZCIsCiJyZXF1aXJlZCI6IGZhbHNlCn0KfQp9LAoiVXJsIjogewoidHlwZSI6ICJSZWNvcmQiLAoiYXR0cmlidXRlcyI6IHsKImhvc3QiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiU3RyaW5nIgp9LAoicGF0aCI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJTdHJpbmciCn0sCiJwcm90b2NvbCI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJTdHJpbmciCn0KfQp9Cn0sCiJlbnRpdHlUeXBlcyI6IHsKIkFjY2Vzc190b2tlbiI6IHsKInNoYXBlIjogewoidHlwZSI6ICJSZWNvcmQiLAoiYXR0cmlidXRlcyI6IHsKImFjciI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJTdHJpbmciLAoicmVxdWlyZWQiOiBmYWxzZQp9LAoiYXVkIjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlN0cmluZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJhdXRoX3RpbWUiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiTG9uZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJjbGllbnRfaWQiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiU3RyaW5nIiwKInJlcXVpcmVkIjogZmFsc2UKfSwKImNvZGUiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiU3RyaW5nIiwKInJlcXVpcmVkIjogZmFsc2UKfSwKImV4cCI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJMb25nIiwKInJlcXVpcmVkIjogZmFsc2UKfSwKImlhdCI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJMb25nIiwKInJlcXVpcmVkIjogZmFsc2UKfSwKImlzcyI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJUcnVzdGVkSXNzdWVyIgp9LAoianRpIjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlN0cmluZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJuYmYiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiTG9uZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJzY29wZSI6IHsKInR5cGUiOiAiU2V0IiwKImVsZW1lbnQiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiU3RyaW5nIgp9LAoicmVxdWlyZWQiOiBmYWxzZQp9LAoic3RhdHVzIjogewoidHlwZSI6ICJSZWNvcmQiLAoiYXR0cmlidXRlcyI6IHsKInN0YXR1c19saXN0IjogewoidHlwZSI6ICJSZWNvcmQiLAoiYXR0cmlidXRlcyI6IHsKImlkeCI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJMb25nIgp9LAoidXJpIjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlN0cmluZyIKfQp9Cn0KfSwKInJlcXVpcmVkIjogZmFsc2UKfSwKInN1YiI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJTdHJpbmciLAoicmVxdWlyZWQiOiBmYWxzZQp9LAoidG9rZW5fdHlwZSI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJTdHJpbmciLAoicmVxdWlyZWQiOiBmYWxzZQp9LAoidXNlcm5hbWUiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiU3RyaW5nIiwKInJlcXVpcmVkIjogZmFsc2UKfSwKIng1dCNTMjU2IjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlN0cmluZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0KfQp9Cn0sCiJJc3N1ZSI6IHsKInNoYXBlIjogewoidHlwZSI6ICJSZWNvcmQiLAoiYXR0cmlidXRlcyI6IHsKImFwcF9pZCI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJTdHJpbmciLAoicmVxdWlyZWQiOiBmYWxzZQp9LAoiaWQiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiU3RyaW5nIiwKInJlcXVpcmVkIjogZmFsc2UKfSwKIm5hbWUiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiU3RyaW5nIiwKInJlcXVpcmVkIjogZmFsc2UKfSwKInBlcm1pc3Npb24iOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiU3RyaW5nIiwKInJlcXVpcmVkIjogZmFsc2UKfSwKInN1YiI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJTdHJpbmciCn0KfQp9Cn0sCiJSb2xlIjoge30sCiJUcnVzdGVkSXNzdWVyIjogewoic2hhcGUiOiB7CiJ0eXBlIjogIlJlY29yZCIsCiJhdHRyaWJ1dGVzIjogewoiaXNzdWVyX2VudGl0eV9pZCI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJVcmwiCn0KfQp9Cn0sCiJVc2VyIjogewoibWVtYmVyT2ZUeXBlcyI6IFsKIlJvbGUiCl0sCiJzaGFwZSI6IHsKInR5cGUiOiAiUmVjb3JkIiwKImF0dHJpYnV0ZXMiOiB7CiJlbWFpbCI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJTdHJpbmciLAoicmVxdWlyZWQiOiBmYWxzZQp9LAoiaWRfdG9rZW4iOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiSWRfdG9rZW4iLAoicmVxdWlyZWQiOiBmYWxzZQp9LAoicm9sZSI6IHsKInR5cGUiOiAiU2V0IiwKImVsZW1lbnQiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiU3RyaW5nIgp9LAoicmVxdWlyZWQiOiBmYWxzZQp9LAoic3ViIjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlN0cmluZyIKfSwKInVzZXJpbmZvX3Rva2VuIjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlVzZXJpbmZvX3Rva2VuIiwKInJlcXVpcmVkIjogZmFsc2UKfSwKInVzZXJuYW1lIjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlN0cmluZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0KfQp9Cn0sCiJVc2VyaW5mb190b2tlbiI6IHsKInNoYXBlIjogewoidHlwZSI6ICJSZWNvcmQiLAoiYXR0cmlidXRlcyI6IHsKImFjciI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJTdHJpbmciLAoicmVxdWlyZWQiOiBmYWxzZQp9LAoiYW1yIjogewoidHlwZSI6ICJTZXQiLAoiZWxlbWVudCI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJTdHJpbmciCn0sCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJhdWQiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiU3RyaW5nIiwKInJlcXVpcmVkIjogZmFsc2UKfSwKImVtYWlsIjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlN0cmluZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJlbWFpbF92ZXJpZmllZCI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJCb29sIiwKInJlcXVpcmVkIjogZmFsc2UKfSwKImV4cCI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJMb25nIiwKInJlcXVpcmVkIjogZmFsc2UKfSwKImZhbWlseV9uYW1lIjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlN0cmluZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJnaXZlbl9uYW1lIjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlN0cmluZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJpYXQiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiTG9uZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJpbnVtIjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlN0cmluZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJpc3MiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiVHJ1c3RlZElzc3VlciIKfSwKImphbnNBZG1pblVJUm9sZSI6IHsKInR5cGUiOiAiU2V0IiwKImVsZW1lbnQiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiU3RyaW5nIgp9LAoicmVxdWlyZWQiOiBmYWxzZQp9LAoianRpIjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlN0cmluZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJtaWRkbGVfbmFtZSI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJTdHJpbmciLAoicmVxdWlyZWQiOiBmYWxzZQp9LAoibmFtZSI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJTdHJpbmciLAoicmVxdWlyZWQiOiBmYWxzZQp9LAoibmJmIjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIkxvbmciLAoicmVxdWlyZWQiOiBmYWxzZQp9LAoibmlja25hbWUiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiU3RyaW5nIiwKInJlcXVpcmVkIjogZmFsc2UKfSwKInJvbGUiOiB7CiJ0eXBlIjogIlNldCIsCiJlbGVtZW50IjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlN0cmluZyIKfSwKInJlcXVpcmVkIjogZmFsc2UKfSwKInN1YiI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJTdHJpbmciLAoicmVxdWlyZWQiOiBmYWxzZQp9LAoidXBkYXRlZF9hdCI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJMb25nIiwKInJlcXVpcmVkIjogZmFsc2UKfSwKInVzZXJuYW1lIjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlN0cmluZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0KfQp9Cn0sCiJXb3JrbG9hZCI6IHsKInNoYXBlIjogewoidHlwZSI6ICJSZWNvcmQiLAoiYXR0cmlidXRlcyI6IHsKImFjY2Vzc190b2tlbiI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJBY2Nlc3NfdG9rZW4iLAoicmVxdWlyZWQiOiBmYWxzZQp9LAoiYXVkIjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlN0cmluZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJjbGllbnRfaWQiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiU3RyaW5nIiwKInJlcXVpcmVkIjogZmFsc2UKfSwKInN1YiI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJTdHJpbmciLAoicmVxdWlyZWQiOiBmYWxzZQp9LAoiaXNzIjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlRydXN0ZWRJc3N1ZXIiLAoicmVxdWlyZWQiOiBmYWxzZQp9Cn0KfQp9LAoiSWRfdG9rZW4iOiB7CiJzaGFwZSI6IHsKInR5cGUiOiAiUmVjb3JkIiwKImF0dHJpYnV0ZXMiOiB7CiJhY3IiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiU3RyaW5nIiwKInJlcXVpcmVkIjogZmFsc2UKfSwKImFtciI6IHsKInR5cGUiOiAiU2V0IiwKImVsZW1lbnQiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiU3RyaW5nIgp9LAoicmVxdWlyZWQiOiBmYWxzZQp9LAoiYXRfaGFzaCI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJTdHJpbmciLAoicmVxdWlyZWQiOiBmYWxzZQp9LAoiYXVkIjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlN0cmluZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJhdXRoX3RpbWUiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiTG9uZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJjX2hhc2giOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiU3RyaW5nIiwKInJlcXVpcmVkIjogZmFsc2UKfSwKImV4cCI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJMb25nIiwKInJlcXVpcmVkIjogZmFsc2UKfSwKImdyYW50IjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlN0cmluZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJpYXQiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiTG9uZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJpc3MiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiVHJ1c3RlZElzc3VlciIKfSwKImphbnNPcGVuSURDb25uZWN0VmVyc2lvbiI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJTdHJpbmciLAoicmVxdWlyZWQiOiBmYWxzZQp9LAoianRpIjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlN0cmluZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJuYmYiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiTG9uZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJub25jZSI6IHsKInR5cGUiOiAiRW50aXR5T3JDb21tb24iLAoibmFtZSI6ICJTdHJpbmciLAoicmVxdWlyZWQiOiBmYWxzZQp9LAoic2lkIjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlN0cmluZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0sCiJzdGF0dXMiOiB7CiJ0eXBlIjogIlJlY29yZCIsCiJhdHRyaWJ1dGVzIjogewoic3RhdHVzX2xpc3QiOiB7CiJ0eXBlIjogIlJlY29yZCIsCiJhdHRyaWJ1dGVzIjogewoiaWR4IjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIkxvbmciCn0sCiJ1cmkiOiB7CiJ0eXBlIjogIkVudGl0eU9yQ29tbW9uIiwKIm5hbWUiOiAiU3RyaW5nIgp9Cn0KfQp9LAoicmVxdWlyZWQiOiBmYWxzZQp9LAoic3ViIjogewoidHlwZSI6ICJFbnRpdHlPckNvbW1vbiIsCiJuYW1lIjogIlN0cmluZyIsCiJyZXF1aXJlZCI6IGZhbHNlCn0KfQp9Cn0sCiJUZXN0UHJpbmNpcGFsMSI6IHsKInNoYXBlIjogewoidHlwZSI6ICJSZWNvcmQiLAoiYXR0cmlidXRlcyI6IHsKImlzX29rIjogewoidHlwZSI6ICJCb29sIiwKInJlcXVpcmVkIjogdHJ1ZQp9Cn0KfQp9LAoiVGVzdFByaW5jaXBhbDIiOiB7CiJzaGFwZSI6IHsKInR5cGUiOiAiUmVjb3JkIiwKImF0dHJpYnV0ZXMiOiB7CiJpc19vayI6IHsKInR5cGUiOiAiQm9vbCIsCiJyZXF1aXJlZCI6IHRydWUKfQp9Cn0KfQp9LAoiYWN0aW9ucyI6IHsKIlVwZGF0ZSI6IHsKImFwcGxpZXNUbyI6IHsKInJlc291cmNlVHlwZXMiOiBbCiJJc3N1ZSIKXSwKInByaW5jaXBhbFR5cGVzIjogWwoiV29ya2xvYWQiLAoiVXNlciIKXSwKImNvbnRleHQiOiB7CiJ0eXBlIjogIkNvbnRleHQiCn0KfQp9LAoiVXBkYXRlVGVzdFByaW5jaXBhbCI6IHsKImFwcGxpZXNUbyI6IHsKInJlc291cmNlVHlwZXMiOiBbCiJJc3N1ZSIKXSwKInByaW5jaXBhbFR5cGVzIjogWwoiVGVzdFByaW5jaXBhbDEiLAoiVGVzdFByaW5jaXBhbDIiCl0sCiJjb250ZXh0IjogewoidHlwZSI6ICJSZWNvcmQiLAoiYXR0cmlidXRlcyI6IHt9Cn0KfQp9Cn0KfQp9"
    }
  }
}
```

## action.txt

```declarative
Jans::Action::"Update"
```

## context.json

```declarative
{}
```

## principals.json

```declarative
{
    "type": "Jans::User",
    "id": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
    "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
    "role":"CasaAdmin"
}
```

## resource.json

```declarative
{
  "app_id": "admin_ui_id",
  "id": "admin_ui_id",
  "name": "My App",
  "permission": "view_clients",
  "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
  "type": "Jans::Issue",
  "loc": "US"
}
```

## sample_cedarling_post_authn.txt

```declarative
import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.postauthn.PostAuthnType;
import io.jans.service.custom.script.CustomScriptManager;
import uniffi.cedarling_uniffi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.cedarling.binding.wrapper.CedarlingAdapter;
import org.json.JSONObject;
import org.apache.commons.lang3.StringUtils;

public class PostAuthn implements PostAuthnType {

    private static final Logger log = LoggerFactory.getLogger(CustomScriptManager.class);
    CedarlingAdapter cedarlingAdapter = null;
    String action = null;
    String resourceStr = null;
    String contextStr = null;
    String principalsStr = null;

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Post Authentication. Initializing...");
        log.info("Post Authentication. Initialized");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Post Authentication. Initializing...");
        log.info("Post Authentication. Initialized");
        if(!configurationAttributes.containsKey("BOOTSTRAP_JSON_PATH")) {
            log.error("Initialization. Property bootstrap_file_path is not specified.");
            return true;
        }
        log.info("Initialize Cedarling...");

        // Read input files for authorization
        String bootstrapFilePath = configurationAttributes.get("BOOTSTRAP_JSON_PATH").getValue2();
        String actionFilePath = configurationAttributes.get("ACTION_FILE_PATH").getValue2();
        String resourceFilePath = configurationAttributes.get("RESOURCE_FILE_PATH").getValue2();
        String contextFilePath = configurationAttributes.get("CONTEXT_FILE_PATH").getValue2();
        String principalsFilePath = configurationAttributes.get("PRINCIPALS_FILE_PATH").getValue2();

        String bootstrapJson = null;
        try {
            bootstrapJson = readFile(bootstrapFilePath);
            action = readFile(actionFilePath);
            resourceStr = readFile(resourceFilePath);
            contextStr = readFile(contextFilePath);
            principalsStr = readFile(principalsFilePath);
            cedarlingAdapter = new CedarlingAdapter();
            cedarlingAdapter.loadFromJson(bootstrapJson);
        } catch (CedarlingException e) {
            log.error("Unable to initialize Cedarling" + e.getMessage());
            return true;
        } catch (Exception e) {
            log.error("Unable to initialize Cedarling" + e.getMessage());
            return true;
        }
            log.info("Cedarling Initialization successful...");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Post Authentication. Destroying...");
        log.info("Post Authentication. Destroyed.");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }

    @Override
    public boolean forceReAuthentication(Object context) {
        return false;
    }

    @Override
    public boolean forceAuthorization(Object context) {
        log.info("Inside forceAuthorization method...");
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;
        try {
            List<EntityData> principalsJson = List.of(EntityData.Companion.fromJson(principalsStr));
            JSONObject resourceJson = new JSONObject(resourceStr);
            JSONObject contextJson = new JSONObject(contextStr);

            AuthorizeResult result = cedarlingAdapter.authorizeUnsigned(principalsJson, action, resourceJson, contextJson);
            cedarlingAdapter.close();
            log.info("Cedarling Authz Response Decision: " + result.getDecision());
            //logic to to use the Cedarling authorization decision ...
        } catch(AuthorizeException | EntityException e) {
            log.error("Error in Cedarling Authz: " + e.getMessage());
            return false;
        }
        return false;
    }

    public String readFile(String filePath) {
        Path path = Paths.get(filePath).toAbsolutePath();
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
```