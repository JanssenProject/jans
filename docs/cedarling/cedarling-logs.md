---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - logging
  - audit
---


# Cedarling Logs

## Cedarling Audit Logs

The Cedarling logs contains a record of all a Cedarling's decisions and token validations.
Cedarling has four logging options, which are configurable via the `CEDARLING_LOG_TYPE`
bootstrap property:

* `off` - no logging
* `memory` - logs stored in Cedarling in-memory KV store, fetched by client via logging interface. This
  is ideal for batching logs without impeding authz performance
* `std_out` - write logs synchronously to std_out
* `lock` - periodically POST logs to Jans Lock Server `/audit` endpoint for central archiving.

There are three different log records produced by the Cedarling:

* `Decision` - The result and diagnostics of an authz decision
* `System` - Startup, debug and other Cedarling messages not related to authz
* `Metric`- Performance and usage data

## Jans Lock Server

In enterprise deployments, [Janssen Lock Server](../janssen-server/lock/) collects Cedarling
logs and can stream to a database or S3 bucket. The Cedarling decision logs provide compliance
evidence of usage of the domain's externalized policies. The logs are also useful for forensic
analysis to show everything the attacker attempted, both allowed and denied.

## Example of log

The JSON in this document is formatted for readability but is not prettified in the actual implementation.  
The result of the authorization is quite extensive because we log all `cedar-policy` entity information for forensic analysis. We cannot truncate the data, as it may contain critical information.

```json
{
    "id": "01937015-462d-7727-b789-ed95f7faf7a4",
    "time": 1732752262,
    "log_kind": "System",
    "pdp_id": "75f0dc93-0a90-4076-95fa-dc16d3f00375",
    "msg": "configuration parsed successfully"
}
{
    "id": "01937015-462f-7cb5-86bb-d06c56dc5ab3",
    "time": 1732752262,
    "log_kind": "System",
    "pdp_id": "75f0dc93-0a90-4076-95fa-dc16d3f00375",
    "msg": "Cedarling Authz initialized successfully",
    "application_id": "TestApp"
}      
{
    "id": "01937015-4649-7aad-8df8-4976e4bd8565",
    "time": 1732752262,
    "log_kind": "Decision",
    "pdp_id": "75f0dc93-0a90-4076-95fa-dc16d3f00375",
    "msg": "Result of authorize.",
    "application_id": "TestApp",
    "action": "Jans::Action::\"Read\"",
    "resource": "Jans::Application::\"some_id\"",
    "context": {
        "user_agent": "Linux",
        "operating_system": "Linux",
        "network_type": "Local",
        "network": "127.0.0.1",
        "geolocation": [
            "America"
        ],
        "fraud_indicators": [
            "Allowed"
        ],
        "device_health": [
            "Healthy"
        ],
        "current_time": 1732752262
    },
    "entities": [
        {
            "uid": {
                "type": "Jans::Action",
                "id": "PUT"
            },
            "attrs": {},
            "parents": []
        },
        {
            "uid": {
                "type": "Jans::Action",
                "id": "Share"
            },
            "attrs": {},
            "parents": []
        },
        {
            "uid": {
                "type": "Jans::Action",
                "id": "PATCH"
            },
            "attrs": {},
            "parents": []
        },
        {
            "uid": {
                "type": "Jans::Action",
                "id": "HEAD"
            },
            "attrs": {},
            "parents": []
        },
        {
            "uid": {
                "type": "Jans::Action",
                "id": "Monitor"
            },
            "attrs": {},
            "parents": []
        },
        {
            "uid": {
                "type": "Jans::Action",
                "id": "Compare"
            },
            "attrs": {},
            "parents": []
        },
        {
            "uid": {
                "type": "Jans::User",
                "id": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0"
            },
            "attrs": {
                "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
                "role": [
                    "CasaAdmin"
                ],
                "email": {
                    "domain": "jans.test",
                    "uid": "admin"
                }
            },
            "parents": [
                {
                    "type": "Jans::Role",
                    "id": "CasaAdmin"
                }
            ]
        },
        {
            "uid": {
                "type": "Jans::id_token",
                "id": "ijLZO1ooRyWrgIn7cIdNyA"
            },
            "attrs": {
                "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
                "acr": "simple_password_auth",
                "exp": 1731956630,
                "jti": "ijLZO1ooRyWrgIn7cIdNyA",
                "amr": [],
                "aud": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
                "iss": {
                    "__entity": {
                        "type": "Jans::TrustedIssuer",
                        "id": "https://account.gluu.org"
                    }
                },
                "iat": 1731953030
            },
            "parents": []
        },
        {
            "uid": {
                "type": "Jans::Action",
                "id": "Test"
            },
            "attrs": {},
            "parents": []
        },
        {
            "uid": {
                "type": "Jans::Workload",
                "id": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62"
            },
            "attrs": {
                "iss": {
                    "__entity": {
                        "type": "Jans::TrustedIssuer",
                        "id": "https://account.gluu.org"
                    }
                },
                "client_id": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62"
            },
            "parents": []
        },
        {
            "uid": {
                "type": "Jans::Action",
                "id": "Write"
            },
            "attrs": {},
            "parents": []
        },
        {
            "uid": {
                "type": "Jans::Action",
                "id": "Read"
            },
            "attrs": {},
            "parents": []
        },
        {
            "uid": {
                "type": "Jans::Role",
                "id": "CasaAdmin"
            },
            "attrs": {},
            "parents": []
        },
        {
            "uid": {
                "type": "Jans::Action",
                "id": "Search"
            },
            "attrs": {},
            "parents": []
        },
        {
            "uid": {
                "type": "Jans::Application",
                "id": "some_id"
            },
            "attrs": {
                "name": "Some Application",
                "app_id": "application_id",
                "url": {
                    "host": "jans.test",
                    "path": "/protected-endpoint",
                    "protocol": "http"
                }
            },
            "parents": []
        },
        {
            "uid": {
                "type": "Jans::Action",
                "id": "GET"
            },
            "attrs": {},
            "parents": []
        },
        {
            "uid": {
                "type": "Jans::Action",
                "id": "Execute"
            },
            "attrs": {},
            "parents": []
        },
        {
            "uid": {
                "type": "Jans::Access_token",
                "id": "uZUh1hDUQo6PFkBPnwpGzg"
            },
            "attrs": {
                "nbf": 1731953030,
                "scope": [
                    "email",
                    "openid",
                    "profile",
                    "role"
                ],
                "exp": 1732121460,
                "aud": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
                "iss": {
                    "__entity": {
                        "type": "Jans::TrustedIssuer",
                        "id": "https://account.gluu.org"
                    }
                },
                "jti": "uZUh1hDUQo6PFkBPnwpGzg",
                "iat": 1731953030
            },
            "parents": []
        },
        {
            "uid": {
                "type": "Jans::Userinfo_token",
                "id": "OIn3g1SPSDSKAYDzENVoug"
            },
            "attrs": {
                "aud": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
                "jti": "OIn3g1SPSDSKAYDzENVoug",
                "email": {
                    "dost",
                    "uid": "admst",
                    "uid": "admst",
                    "uid": "admst",
                    "uid": "admin"
                },
                "name": "Default Admin User",
                "iss": {
                    "__entity": {
                        "type": "Jans::TrustedIssuer",
                        "id": "https://account.gluu.org"
                    }
                },
                "role": [
                    "CasaAdmin"
                ],
                "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0"
            },
            "parents": []
        },
        {
            "uid": {
                "type": "Jans::Action",
                "id": "DELETE"
            },
            "attrs": {},
            "parents": []
        },
        {
            "uid": {
                "type": "Jans::Action",
                "id": "Tag"
            },
            "attrs": {},
            "parents": []
        }
    ],
    "person_principal": "Jans::User::\"qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0\"",
    "person_diagnostics": {
        "reason": [
            {
                "id": "840da5d85403f35ea76519ed1a18a33989f855bf1cf8",
                "description": "simple policy example for principal user"
            }
        ],
        "errors": []
    },
    "person_decision": "ALLOW",
    "workload_principal": "Jans::Workload::\"d7f71bea-c38d-4caf-a1ba-e43c74a11a62\"",
    "workload_diagnostics": {
        "reason": [
            {
                "id": "444da5d85403f35ea76519ed1a18a33989f855bf1cf8",
                "description": "simple policy example for principal workload"
            }
        ],
        "errors": []
    },
    "workload_decision": "ALLOW",
    "authorized": true
}
```
