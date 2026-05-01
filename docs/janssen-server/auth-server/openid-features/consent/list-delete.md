---
tags:
  - administration
  - auth-server
  - openidc
  - feature
  - consent
---
# List/Delete Consent

Janssen stores authorization/consent decisions in token grant data. To view and revoke them, use the Config API token endpoints.

As noted in Janssen planning docs, viewing and revoking consent is done via Config API (not OpenID Connect or SCIM):

- [Consent Gathering (planning context)](../../../planning/use-cases.md)
- [Config API overview](../../../config-guide/config-tools/config-api/README.md)

## How Janssen Persists Consent

In auth server code, consent outcomes are applied through authorization grants and persisted token entities. Those entities include useful fields for consent-oriented filtering, such as:

- `usrId` / `jansUsrDN` (user)
- `clnId` (client)
- `scp` (scopes)
- `grtId`, `grtTyp`, `tknTyp`

## List Consents

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

## Delete (Revoke) Consent

To revoke a consent/grant entry, revoke its token by token code:

```text
DELETE /api/v1/token/revoke/{tknCde}
```

After revocation, the corresponding token entry is removed. Repeating this for all tokens associated with a user/client effectively removes previously stored consent approvals for that pairing.

## Notes

- Config API endpoints are OAuth 2.0 protected; ensure your admin/API client has required Config API scopes.
- In production, prefer automation that first searches by user/client, then selectively revokes matching entries.
