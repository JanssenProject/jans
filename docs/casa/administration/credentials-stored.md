---
tags:
- Casa
- administration
- credential storage
---

# Storage of User Credentials

The following provides a summary of where user credentials can be found in LDAP. If you need information regarding another backend database, please open a support ticket.

## FIDO 2 devices
Relevant information can be found under `fido2_register` branch of the user's entry.

## TOTP / HOTP devices
TOTP/HOTP device information is stored in the `jansExternalUid` attribute as well as in `jansOTPDevices`.

## Phone Numbers
Verified mobile phone numbers are stored in the `mobile` attribute of the user entry. Associated information (date added, nickname of device, etc.) is stored in JSON format in the `jansMobileDevices` attribute.

## 2FA enforcement policy

When administrators allow users to set their own strong authentication policy, that is, users being able to decide if 2FA authentication always takes place or only when device/location used is not recognized, the attributes involved are `jansStrongAuthPolicy` and `jansTrustedDevicesInfo`. The former contains the user preference, and the latter the information of his trusted devices and locations. 

For privacy reasons, data stored in `jansTrustedDevicesInfo` is encoded so the only applicable operation upon this data is flushing the list; this can be achieved by removing the attribute entirely.
