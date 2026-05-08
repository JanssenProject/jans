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
- Authorization endpoint behavior in Janssen: [Authorization Endpoint](../endpoints/authorization.md)

In Janssen Server, consent happens during the authorization flow after user authentication. By default, users are shown requested scopes and can allow or deny access.

## How consent is handled in Janssen

- **Default consent UI**: Janssen shows consent screens in the authorization flow.
- **Prompt control**: clients can request explicit consent with `prompt=consent`.
- **When consent can be skipped**: server logic may skip the consent page in specific cases, such as trusted clients or when prior consent and scope conditions are already satisfied.
- **Persistence**: user approvals are represented through authorization/token grant data, which can be queried and revoked via Config API token endpoints.

## Consent administration

### End-user self-service

Jans Casa ("Casa") is a self-service web portal for end-users to manage authentication and authorization preferences for their account in a Janssen Server.

Casa's [consent management plugin](../../../casa/plugins/consent-management.md) gives end-users the ability to view and revoke previously granted authorizations provided to applications accessed with their account in a Janssen Server.

### Using API

Janssen config API allows applications to administer consent via REST APIs. Janssen stores authorization/consent decisions in token grant data. To view and revoke them, use the Config API token endpoints.

As noted in Janssen planning docs, viewing and revoking consent is done via Config API (not OpenID Connect or SCIM):

- [Consent Gathering (planning context)](../../planning/use-cases.md)
- [Config API overview](../../config-guide/config-tools/config-api/README.md)


#### List Consents

Use Config API token search to list grants/tokens by user and/or client.

Endpoint pattern:

```text
GET /api/v1/token/search
```

Example query parameters:

- `fieldValuePair=usrId=<userId>`
- `fieldValuePair=clnId=<clientId>`
- `fieldValuePair=usrId=<userId>,clnId=<clientId>`
- optional paging/sorting: `startIndex`, `limit`, `sortBy`, `sortOrder`

You can also fetch tokens for a specific client:

```text
GET /api/v1/token/client/{clientId}
```

#### Delete (Revoke) Consent

To revoke a consent/grant entry, revoke its token by token code:

```text
DELETE /api/v1/token/revoke/{tknCde}
```

After revocation, the corresponding token entry is removed. Repeating this for all tokens associated with a user/client effectively removes previously stored consent approvals for that pairing.


## Customize

Janssen supports customizing consent management beyond the default implementation. This can be achieved either by using interception scripts or by using Agama-based consent flows.

Refer to [Consent Gathering](../../../script-catalog/consent_gathering/consent-gathering.md) for implementation details and examples.

## Related OpenID Connect Behavior

- `prompt=consent` requests explicit consent interaction in the authorization flow.
- `prompt=none` does not allow interactive consent and returns an error if consent is required.

See also:

- [Authorization Endpoint](../endpoints/authorization.md)
- [Prompt Parameter](./prompt-parameter.md)
