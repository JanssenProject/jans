---
tags:
  - administration
  - fido2
  - attestation
  - diagnostics
---

# Attestation Rejection Diagnostics

When attestation rejects a registration, the reason reaches the end user as a generic failure and is
recorded in metrics as the raw exception message. An unknown AAGUID, an untrusted root certificate and
an authenticator blocked by an MDS status report all look alike, so rejections cannot be counted by
cause.

The FIDO2 server records a stable diagnostic code against the metrics entry instead, and exposes an
endpoint that groups rejections by that code.

!!! note
    Nothing about rejection behaviour changes — only the reason that gets recorded. The response
    returned to the client is unchanged: `ErrorResponseFactory` still returns
    `{"status": "failed", "errorMessage": "…"}`, and the codes never appear in the client body.

## Rejection analytics

=== "FIDO2 server"

    ```
    GET /jans-fido2/restv1/metrics/analytics/attestation-rejections?startTime=&endTime=
    ```

=== "Config API (fido2 plugin)"

    ```
    GET /fido2/metrics/analytics/attestation-rejections?start_date=&end_date=
    ```

    Served under the Config API base path. Requires one of the
    `https://jans.io/oauth/config/fido2-metrics.readonly`,
    `https://jans.io/oauth/config/fido2.write`,
    `https://jans.io/oauth/config/fido2.admin`,
    `https://jans.io/oauth/config/read-all` or
    `https://jans.io/oauth/config/write-all` scopes.

```json
{
    "totalRejections": 18,
    "registrationAttempts": 150,
    "rejectionRate": 0.12,
    "reasonCodes": {
        "JFS_AAGUID_NOT_IN_MDS": 14,
        "JFS_ROOT_CERT_NOT_TRUSTED": 3,
        "JFS_AUTHENTICATOR_STATUS_UNACCEPTABLE": 1
    }
}
```

## Diagnostic codes

| Code | Cause | What to check |
| --- | --- | --- |
| `JFS_AAGUID_NOT_IN_MDS` | The authenticator's AAGUID is not in the loaded metadata | Whether metadata loaded at all; [Vendor Metadata](./vendor-metadata.md) to supply it locally |
| `JFS_AUTHENTICATOR_STATUS_UNACCEPTABLE` | An MDS status report marks the authenticator as unacceptable | The authenticator's status in the FIDO metadata |
| `JFS_ROOT_CERT_NOT_TRUSTED` | The attestation certificate chain did not verify to a trusted root | Configured root certificates for that vendor |
| `JFS_ATTESTATION_FORMAT_NOT_PERMITTED` | The attestation statement format is not accepted | The attestation format the authenticator uses |
| `JFS_MDS_UNAVAILABLE` | Metadata was required but could not be fetched or loaded | Server logs for the last metadata refresh |

A failure that is not trust related keeps its original message, and the original message is still
written to the log whenever a code is substituted, so no detail is lost.

## How the codes are stored

Codes are written into the existing `errorReason` field of the metrics entry, with `errorCategory` set
to `ATTESTATION_TRUST`. No new store is introduced, so rejections also appear in the general
[`analytics/errors`](./passkey-telemetry.md) breakdown under that category.

Because `errorReason` normally holds prose, a client that displays it should treat a value prefixed
`JFS_` as a code to map rather than a sentence to show.

## Reading the rejection rate

`rejectionRate` is `totalRejections` divided by `registrationAttempts` over the same range.

An attempt and the rejection that follows it are separate records with their own timestamps, so a
narrow range can contain one without the other. Rather than publish a misleading number, the endpoint
reports:

- the computed rate, when there are attempts in range and rejections do not exceed them;
- `1.0` plus a `rejectionRateNote`, when rejections exceed attempts because some attempts fall before
  the range;
- `null` plus a `rejectionRateNote`, when no attempts were recorded in range at all.

Widening the range resolves both boundary cases.
