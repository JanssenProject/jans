---
tags:
  - administration
  - fido2
  - attestation
  - diagnostics
---

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

A stale or failed metadata load is a common cause of "an authenticator that worked yesterday is
suddenly rejected". Until now those failures were written to the server log and then discarded, leaving
log-grepping as the only way to find them.

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
    "lastSuccessfulRefresh": "2026-08-01T04:15:22",
    "metadataServers": [
        {
            "url": "https://mds.fidoalliance.org/",
            "rootCertConfigured": false
        }
    ],
    "timestamp": "2026-08-07T09:31:04.118"
}
```

The endpoint is read-only in the strict sense: it reports in-memory state only, and never triggers a
metadata download or reads the document store. It is safe to poll.

### Status and HTTP codes

| `status` | HTTP | Meaning |
| --- | --- | --- |
| `UP` | 200 | Metadata is loaded and inside its validity window. |
| `DOWN` | 503 | The metadata is unusable — nothing is loaded, or the loaded blob is past its `nextUpdate`. |
| `DISABLED` | 200 | `disableMetadataService` is set. |

`DISABLED` deliberately returns 200. A metadata service switched off by configuration is a choice, not
an outage, and should not page anyone when this endpoint is wired to a monitor.

The `DOWN` body is returned with the 503, so a monitor that captures the response gets
`lastRefreshError` and `tocEntryCount` along with the status code.

!!! note
    **A failed refresh on its own is not `DOWN`.** While the cached blob is still inside its validity
    window, attestation validation keeps working normally off that cached metadata, so returning 503
    would page an operator for a service that is functioning. The failure is still reported — as
    `lastRefreshError` on a 200 response. Alert on that field if you want early warning that the
    metadata is heading towards expiry, and on `status` for the outage itself.

### Fields

| Field | Meaning |
| --- | --- |
| `tocEntryCount` | Authenticator metadata entries currently loaded in memory. Zero means attestation has nothing to validate against. |
| `nextUpdate` | The `nextUpdate` declared by the loaded TOC blob. Absent when no blob has been parsed since startup. |
| `blobExpired` | `true` when no blob is loaded, or its `nextUpdate` is today or earlier — a re-download is due. This is the same rule the server applies when deciding whether to download. |
| `lastSuccessfulRefresh` | When metadata was last downloaded and parsed successfully (UTC). Absent when no refresh has succeeded since startup. |
| `lastRefreshError` | Why the most recent refresh failed. Absent when the last refresh succeeded. |
| `metadataServers[].rootCertConfigured` | Whether a per-endpoint trust anchor is configured for that endpoint. |

`lastRefreshError` reports the **first** failure of the attempt, because that is the root cause: a
failed download is followed by a fallback to the cached blob, which tends to fail as well. Reporting
the later failure would describe an unreachable endpoint as an unparseable document.

`metadataServers[].rootCertConfigured` is a presence flag only. The configured certificate is never
included in the response.

## Troubleshooting

**Enforcement does not seem to apply.** Check `attestationMode` and `attestationModeRecognized`. An
unrecognised mode value silently leaves the server lenient.

**Unapproved authenticators are being registered.** The default `monitor` mode does not reject them.
Check `unattestedAuthenticatorsAllowed`; only `enforced` will keep them out.

**Certain authenticators are rejected while others succeed.** With `attestationMode` set to `enforced`,
an authenticator whose AAGUID is absent from the loaded metadata is rejected. Check `tocEntryCount` on
the MDS health endpoint — a low or zero count means the metadata did not load. See
[Vendor Metadata](./vendor-metadata.md) for supplying metadata locally.

**Authenticators that worked yesterday are now rejected.** Check MDS health. `blobExpired: true` with
`status: DOWN` means the metadata blob is past its `nextUpdate` and was discarded rather than used, so
under `enforced` mode nothing validates. `lastRefreshError` says why the refresh that should have
replaced it failed.

**MDS health reports `DISABLED`.** `disableMetadataService` is set in the FIDO2 configuration. No
metadata is downloaded and attestation cannot be validated against FIDO metadata.

**Apple devices fail to register.** Check `appleRootCaPresent`. Without the Apple WebAuthn root CA,
Apple anonymous attestation cannot be validated.
