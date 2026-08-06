---
tags:
  - administration
  - fido2
  - attestation
  - diagnostics
---

<!--
  markdownlint-disable MD046
  Code blocks on this page are fenced, matching the rest of docs/janssen-server/fido/ and the
  pymdownx.superfences extension configured in mkdocs.yml. The fences inside the "===" tab containers
  have to be indented four spaces for pymdownx.tabbed to pick them up, which a plain CommonMark linter
  reads as indented code blocks — so the page looks like it mixes both styles when it does not.
  Converting the top-level examples to indented blocks would silence the rule but drop their json
  language tag and with it the syntax highlighting.
-->

# Trust Diagnostics

Trust misconfiguration is one of the harder FIDO2 problems to diagnose, because it reaches the end user
as a generic registration failure. An authenticator that was working yesterday starts being rejected,
and nothing in the response says why.

The FIDO2 server exposes read-only endpoints that make the trust state visible. They are diagnostics
only — they never change attestation behaviour.

## Attestation configuration

=== "FIDO2 server"

    ```
    GET /jans-fido2/restv1/trust/attestation/config
    ```

=== "Config API (fido2 plugin)"

    ```
    GET /fido2/trust/attestation/config
    ```

    Served under the Config API base path. Requires one of the
    `https://jans.io/oauth/config/fido2.readonly`, `fido2.write`, `fido2.admin` or `read-all` scopes.

```json
{
    "attestationMode": "monitor",
    "attestationModeRecognized": true,
    "unattestedAuthenticatorsAllowed": true,
    "enterpriseAttestation": false,
    "metadataServiceDisabled": false,
    "appleRootCaPresent": true,
    "enabledFidoAlgorithms": ["RS256", "ES256"],
    "hints": []
}
```

### Attestation modes

`attestationMode` is set in the [FIDO2 configuration](./fido2-server-properties-config.md) and defaults
to `monitor`.

| Mode | Behaviour |
| --- | --- |
| `disabled` | Attestation is not validated. |
| `monitor` | Attestation is validated and logged, but a failure does **not** reject the registration. |
| `enforced` | A failing attestation check rejects the registration. |

!!! important
    **Only `enforced` rejects a registration on the basis of attestation.** Both `disabled` and
    `monitor` are lenient about attestation, so under the default `monitor` mode an authenticator whose
    attestation cannot be validated — an unknown AAGUID, an expired metadata blob, an untrusted root —
    is still registered successfully. If you are relying on attestation to keep unapproved
    authenticators out, `monitor` will not do it.

    This concerns attestation only. The other registration checks — challenge, origin, RP ID, signature
    — reject an invalid registration in every mode.

    `unattestedAuthenticatorsAllowed` reports this directly: it is `true` for every mode except
    `enforced`.

`attestationModeRecognized` is `false` when the configured value is not one of the three modes above —
for example a typo such as `Enforce`. An unrecognised value leaves the server in lenient behaviour, so
this flag is worth checking when enforcement appears not to be taking effect.

`metadataServiceDisabled` reflects the `disableMetadataService` setting. When it is `true`, attestation
cannot be validated against FIDO metadata at all.

`appleRootCaPresent` reports whether the Apple WebAuthn root CA certificate was loaded at startup. When
it is `false`, Apple anonymous attestation cannot be validated.

## MDS health

=== "FIDO2 server"

    ```
    GET /jans-fido2/restv1/trust/mds/health
    ```

=== "Config API (fido2 plugin)"

    ```
    GET /fido2/trust/mds/health
    ```

    Served under the Config API base path. Requires one of the
    `https://jans.io/oauth/config/fido2.readonly`, `fido2.write`, `fido2.admin` or `read-all` scopes.

```json
{
    "status": "UP",
    "metadataServiceDisabled": false,
    "tocEntryCount": 1284,
    "nextUpdate": "2026-08-15",
    "blobExpired": false,
    "lastSuccessfulRefresh": "2026-07-27T04:15:22",
    "metadataServers": [
        {
            "url": "https://mds.fidoalliance.org/",
            "rootCertConfigured": false
        }
    ],
    "timestamp": "2026-07-27T09:31:04.118"
}
```

| Status | HTTP | Meaning |
| --- | --- | --- |
| `UP` | 200 | Metadata is loaded and within its validity period. |
| `DISABLED` | 200 | `disableMetadataService` is `true`. A deliberate configuration choice, not a failure. |
| `DOWN` | 503 | The blob is expired, no entries are loaded, or the last refresh failed. |

`DISABLED` returns 200 deliberately, so that a deployment which intentionally runs without MDS does not
trigger alerts when this endpoint is wired to a monitor.

Field notes:

- `tocEntryCount` — authenticator metadata entries currently loaded in memory. Zero means nothing can be
  validated against FIDO metadata.
- `nextUpdate` / `blobExpired` — `blobExpired` is `true` when no blob is loaded, or its `nextUpdate` is
  today or earlier. This is the same rule the server uses to decide a re-download is due.
- `lastSuccessfulRefresh` — absent if no refresh has succeeded since startup.
- `lastRefreshError` — present only when the most recent refresh failed; it carries the reason.
- `metadataServers[].rootCertConfigured` — whether a per-endpoint trust anchor is configured. The
  certificate itself is never returned.

## Troubleshooting

**A previously working authenticator is suddenly rejected.** Check `attestationMode` first. Attestation
only rejects a registration under `enforced`; if the mode is `disabled` or `monitor`, the cause lies
elsewhere — look at the challenge, origin, RP ID and signature checks instead.

Under `enforced`, check MDS health next. A stale or failed metadata load is the usual cause of a
previously working authenticator being rejected, and it is invisible from the registration error alone.
Look at `status`, `blobExpired`, and `lastRefreshError`.

**Enforcement does not seem to apply.** Check `attestationMode` and `attestationModeRecognized`. An
unrecognised mode value silently leaves the server lenient.

**Unapproved authenticators are being registered.** The default `monitor` mode does not reject them.
Check `unattestedAuthenticatorsAllowed`; only `enforced` will keep them out.

**Certain authenticators are rejected while others succeed.** With `attestationMode` set to `enforced`,
an authenticator whose AAGUID is absent from the loaded metadata is rejected. Compare `tocEntryCount`
against expectations, and see [Vendor Metadata](./vendor-metadata.md) for supplying metadata locally.

**Apple devices fail to register.** Check `appleRootCaPresent`. Without the Apple WebAuthn root CA,
Apple anonymous attestation cannot be validated.
