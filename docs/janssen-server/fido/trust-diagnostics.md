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
    "lastSuccessfulRefresh": "2026-08-01T04:15:22Z",
    "metadataServers": [
        {
            "url": "https://mds.fidoalliance.org/",
            "rootCertConfigured": false
        }
    ],
    "timestamp": "2026-08-07T09:31:04.118Z"
}
```

The endpoint is read-only in the strict sense: it reports in-memory state only, and never triggers a
metadata download or reads the document store. It is safe to poll.

### Status and HTTP codes

| `status` | HTTP | Meaning |
| --- | --- | --- |
| `UP` | 200 | Metadata is loaded and its `nextUpdate` is still in the future. |
| `DOWN` | 503 | No metadata is loaded, or the loaded blob has reached its `nextUpdate` — today or earlier — and a refresh is overdue. |
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
| `blobExpired` | `true` when no blob is loaded, or its `nextUpdate` is today or earlier — a re-download is due. This is the same rule the server applies when deciding whether to download. It does not mean the blob has been dropped: that only happens once `nextUpdate` has passed. |
| `lastSuccessfulRefresh` | When metadata was last downloaded and parsed successfully, as an ISO-8601 date-time with a UTC offset. Absent when no refresh has succeeded since startup. |
| `lastRefreshError` | Why the most recent refresh failed. Absent when the last refresh succeeded. |
| `metadataServers[].rootCertConfigured` | Whether a per-endpoint trust anchor is configured for that endpoint. |

`lastRefreshError` reports the **first** failure of the attempt, because that is the root cause: a
failed download is followed by a fallback to the cached blob, which tends to fail as well. Reporting
the later failure would describe an unreachable endpoint as an unparseable document.

`metadataServers[].rootCertConfigured` is a presence flag only. The configured certificate is never
included in the response.

## Attestation rejection diagnostics

The attestation configuration tells you what the policy is, and MDS health tells you whether the
metadata behind it loaded. Neither tells you why individual registrations are failing — an unknown
AAGUID, an authenticator blocked by an MDS status report and an untrusted root certificate all reach
the end user as the same generic registration failure.

When attestation is rejected for a trust or metadata reason, the server records a diagnostic code
against the registration in the metrics store, in the existing `errorReason` field, under the
`ATTESTATION_TRUST` error category.

!!! note
    These codes are internal. The response returned to the client is unchanged — still the FIDO
    `{"status": "failed", "errorMessage": "…"}` envelope — and a diagnostic code is never included in
    it. Recording a code changes nothing about whether a registration is rejected.

### Code reference

| Code | Cause | What to do |
| --- | --- | --- |
| `JFS_AAGUID_NOT_IN_MDS` | The authenticator's AAGUID is not in the loaded metadata. | Check `tocEntryCount` on the [MDS health endpoint](#mds-health). If metadata loaded normally, the model genuinely is not in the FIDO Alliance blob — see [Vendor Metadata](./vendor-metadata.md). |
| `JFS_AUTHENTICATOR_STATUS_UNACCEPTABLE` | The authenticator is blocked by an MDS status report — revoked, compromised, or not FIDO certified. | Working as intended. The model is one the FIDO Alliance has flagged. |
| `JFS_ROOT_CERT_NOT_TRUSTED` | The attestation certificate chain did not verify to a trusted root. | Check that the authenticator's root CA is present in the configured `authenticatorCertsFolder`. |
| `JFS_APPLE_ROOT_CA_MISSING` | Apple attestation was attempted with no Apple WebAuthn root CA loaded. | Load the Apple WebAuthn root CA; `appleRootCaPresent` on the attestation config endpoint reports the same condition. Until then every Apple registration fails. |
| `JFS_ATTESTATION_FORMAT_NOT_PERMITTED` | The attestation statement format is not one the server accepts. | Check the attestation formats the deployment supports against what the authenticator sent. |
| `JFS_MDS_UNAVAILABLE` | Metadata was required to validate the authenticator, but none could be obtained. | Check [MDS health](#mds-health) — `lastRefreshError` says why the most recent refresh failed. |
| `JFS_MDS_METADATA_EXPIRED` | The loaded metadata blob was past its `nextUpdate`. | Reserved. An expired blob is discarded at load time rather than used, so an expired blob currently surfaces as `JFS_AAGUID_NOT_IN_MDS` — `blobExpired` on the MDS health endpoint is what confirms expiry is the cause. |

### Rejection analytics

=== "FIDO2 server"

    ```
    GET /jans-fido2/restv1/metrics/analytics/attestation-rejections?startTime=&endTime=
    ```

=== "Config API (fido2 plugin)"

    ```
    GET /fido2/metrics/analytics/attestation-rejections?start_date=&end_date=
    ```

```json
{
    "totalRejections": 37,
    "registrationAttempts": 412,
    "rejectionRate": 0.0898,
    "reasonCodes": {
        "JFS_AAGUID_NOT_IN_MDS": 21,
        "JFS_AUTHENTICATOR_STATUS_UNACCEPTABLE": 9,
        "JFS_ROOT_CERT_NOT_TRUSTED": 5,
        "JFS_ATTESTATION_FORMAT_NOT_PERMITTED": 2
    },
    "topRejectedAaguids": {
        "d8522d9f-575b-4866-88a9-ba99fa02f35b": 14,
        "cb69481e-8ff7-4039-93ec-0a2729a154a8": 9,
        "2fc0579f-8113-47ea-b116-bb5a8db9202a": 7
    }
}
```

This reads the same metrics store as `analytics/errors`, so it requires
[metrics](./passkey-telemetry.md) to be enabled — with metrics off, nothing is recorded and the
endpoint reports zero.

`rejectionRate` is rejections over the registration attempts in the same range. A rejection and the
attempt it belongs to are separate records, so a range can contain one without the other: when no
attempts fall in the range the rate is `null` rather than `0.0`, with a `rejectionRateNote` explaining
why, and when rejections outnumber attempts it is capped at `1.0`. Widen the range for an exact
figure.

`topRejectedAaguids` counts rejections per authenticator model. Rejections not tied to a model — an
attestation format the mode does not permit, for instance — appear in `reasonCodes` only.

!!! important
    Under the default `monitor` mode an authenticator that fails attestation is still **registered**.
    Rejections counted here are the ones that actually failed the registration, which in `monitor`
    mode means the failure came from something other than the lenient attestation path. If you expect
    rejections and see none, check `attestationMode` first.

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
`status: DOWN` means a refresh is overdue. Note the two are not the same thing: a blob is only
discarded once its `nextUpdate` has actually *passed* — on the day itself it is still used. Once it has
passed, the cached blob is dropped rather than used, so under `enforced` mode nothing validates and
`tocEntryCount` falls to `0`. `lastRefreshError` says why the refresh that should have replaced it
failed.

**MDS health reports `DISABLED`.** `disableMetadataService` is set in the FIDO2 configuration. No
metadata is downloaded and attestation cannot be validated against FIDO metadata.

**Apple devices fail to register.** Check `appleRootCaPresent`. Without the Apple WebAuthn root CA,
Apple anonymous attestation cannot be validated.
