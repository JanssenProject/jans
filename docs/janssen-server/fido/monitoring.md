---
tags:
  - administration
  - fido
---

# Monitoring

## FIDO Devices

A FIDO device represents a user credential stored in the Jans Server database that is compliant with the [FIDO](https://fidoalliance.org/) standard. These devices are used as a second factor in a setting of strong authentication.

FIDO devices were superseded by [FIDO 2](#fido-2-devices) devices in Jans Server.

## FIDO2 devices

FIDO2 devices are credentials that adhere to the more current Fido 2.0 initiative (WebAuthn + CTAP). Examples of FIDO2 devices are USB security keys and Super Gluu devices.

The SCIM endpoints for FIDO 2 allow application developers to query, update and delete already existing devices. Addition of devices do not take place through the service since this process requires direct end-user interaction, ie. device enrolling.

The schema attributes for a device of this kind can be found by hitting the URL `https://<jans-server>/jans-scim/restv1/v2/Schemas/urn:ietf:params:scim:schemas:core:2.0:Fido2Device`

To distinguish between regular FIDO2 and SuperGluu devices, note only SuperGluu entries have the attribute `deviceData` populated (i.e. not null)

### Example: Querying Enrolled Devices

Say we are interested in having a list of Super Gluu devices users have enrolled and whose operating system is iOS. We may issue a query like this:

```
curl -k -G -H 'Authorization: Bearer ACCESS_TOKEN' --data-urlencode
'filter=deviceData co "ios"' -d count=10 https://<jans-server>/jans-scim/restv1/v2/Fido2Devices
```

The response will be like:

```
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
