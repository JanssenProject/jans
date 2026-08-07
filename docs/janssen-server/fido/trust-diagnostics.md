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

## Attestation rejection diagnostics

The attestation configuration tells you what the policy is. It does not tell you why individual
registrations are failing — an unknown AAGUID, an authenticator blocked by an MDS status report and an
untrusted root certificate all reach the end user as the same generic registration failure.

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
| `JFS_AAGUID_NOT_IN_MDS` | The authenticator's AAGUID is not in the loaded metadata. | Confirm the metadata actually loaded. If it did, the model genuinely is not in the FIDO Alliance blob — see [Vendor Metadata](./vendor-metadata.md). |
| `JFS_AUTHENTICATOR_STATUS_UNACCEPTABLE` | The authenticator is blocked by an MDS status report — revoked, compromised, or not FIDO certified. | Working as intended. The model is one the FIDO Alliance has flagged. |
| `JFS_ROOT_CERT_NOT_TRUSTED` | The attestation certificate chain did not verify to a trusted root. | Check that the authenticator's root CA is present in the configured `authenticatorCertsFolder`. |
| `JFS_APPLE_ROOT_CA_MISSING` | Apple attestation was attempted with no Apple WebAuthn root CA loaded. | Load the Apple WebAuthn root CA; `appleRootCaPresent` on the attestation config endpoint reports the same condition. Until then every Apple registration fails. |
| `JFS_ATTESTATION_FORMAT_NOT_PERMITTED` | The attestation statement format is not one the server accepts. | Check the attestation formats the deployment supports against what the authenticator sent. |
| `JFS_MDS_UNAVAILABLE` | Metadata was required to validate the authenticator, but none could be obtained. | Check the metadata service configuration and the server log for the last refresh outcome. |
| `JFS_MDS_METADATA_EXPIRED` | The loaded metadata blob was past its `nextUpdate`. | Reserved. An expired blob is discarded at load time rather than used, so an expired blob currently surfaces as `JFS_AAGUID_NOT_IN_MDS` — every lookup against the empty entry map misses. |

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
an authenticator whose AAGUID is absent from the loaded metadata is rejected. See
[Vendor Metadata](./vendor-metadata.md) for supplying metadata locally.

**Apple devices fail to register.** Check `appleRootCaPresent`. Without the Apple WebAuthn root CA,
Apple anonymous attestation cannot be validated.
