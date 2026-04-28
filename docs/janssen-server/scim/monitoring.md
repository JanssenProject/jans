---
tags:
  - administration
  - scim
---

# Monitoring User configuration with SCIM

## How is SCIM data stored?

SCIM [schema spec](https://datatracker.ietf.org/doc/html/rfc7643) does not use LDAP attribute names but a different naming convention for resource attributes (note this is not the case of custom attributes where the SCIM name used is that of the LDAP attribute).

It is possible to determine if a given LDAP attribute is being mapped to a SCIM attribute. For that you need to check in Jans TUI `Auth-Server >> Attributes` and click on any attributes. Check `Include in SCIM Extension:` is `true` or `false`. Whenever you try to map any LDAP attribute to a SCIM attribute keep it's value `true`.

## FIDO Devices

A FIDO device represents a user credential stored in the Jans Server database that is compliant with the [FIDO](https://fidoalliance.org/) standard. These devices are used as a second factor in a setting of strong authentication.

FIDO devices were superseded by [FIDO2](#fido2-devices) devices in Jans Server.

## FIDO2 devices

FIDO2 devices are credentials that adhere to the more current Fido 2.0 initiative (WebAuthn + CTAP). Examples of FIDO2 devices are USB security keys and Super Gluu devices.

The SCIM endpoints for FIDO2 allow application developers to query, update and delete already existing devices. Addition of devices does not take place through the service since this process requires direct end-user interaction, ie. device enrolling.

The schema attributes for a device of this kind can be found by hitting the URL  `https://<jans-server>/jans-scim/restv1/v2/Schemas/urn:ietf:params:scim:schemas:core:2.0:Fido2Device`

To distinguish between regular FIDO2 and SuperGluu devices, note only SuperGluu entries have the attribute `deviceData` populated (i.e. not null)

### Example: Querying Enrolled Devices

Say we are interested in having a list of Super Gluu devices users have enrolled and whose operating system is iOS. We may issue a query like this:

```bash
curl -k -G -H 'Authorization: Bearer ACCESS_TOKEN' --data-urlencode \
'filter=deviceData co "ios"' -d count=10 https://<jans-server>/jans-scim/restv1/v2/Fido2Devices
```

The response will be like:

```json
{
  "totalResults": ...,
  "itemsPerPage": ...,
  "startIndex": 1,
  "schemas": [
    "urn:ietf:params:scim:api:messages:2.0:ListResponse"
  ],
  "Resources": [
    {
      "id": "...",
      "meta": {...},
      "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Fido2Device"],
      "userId": "...",
      ...
      "deviceData": "{...}",
      "displayName": ...,
    },
    ...
  ]
}
```

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
