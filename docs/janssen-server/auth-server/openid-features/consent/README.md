---
tags:
  - administration
  - auth-server
  - openidc
  - feature
  - consent
---

# Consent

Consent is the step where a user approves what a client application can access (for example, requested OpenID Connect/OAuth scopes). In OpenID Connect Core, this behavior is tied to the authorization request and can be explicitly requested with `prompt=consent`.

- OpenID Connect Core specification: [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)
- Authorization endpoint behavior in Janssen: [Authorization Endpoint](../../endpoints/authorization.md)

In Janssen Server, consent happens during the authorization flow after user authentication. By default, users are shown requested scopes and can allow or deny access.

## How consent is handled in Janssen

- **Default consent UI**: Janssen shows consent screens in the authorization flow.
- **Prompt control**: clients can request explicit consent with `prompt=consent`.
- **When consent can be skipped**: server logic may skip the consent page in specific cases, such as trusted clients or when prior consent and scope conditions are already satisfied.
- **Persistence**: user approvals are represented through authorization/token grant data, which can be queried and revoked via Config API token endpoints.

## In This Section

- [Customize](./customize.md): customize consent using Consent Gathering scripts and Agama consent flows.
- [List/Delete Consent](./list-delete.md): list and revoke stored user consents.
