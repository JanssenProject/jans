# Overview

Docker monolith image packaging for Janssen. This image is for test and dev purposes only! This image packs sql and janssen services including, the auth-server, client-api, config-api, fido2, and scim.

## Versions

See [Releases](https://github.com/JanssenProject/docker-jans-monolith/releases) for stable versions. This image should never be used in production.
For bleeding-edge/unstable version, use `janssenproject/monolith:1.0.2_dev`.

## Environment Variables

The following environment variables are supported by the container:

- `CN_HOSTNAME`: Hostname to install janssen with.
- `CN_ADMIN_PASS`: Password of the admin user.
- `CN_ORG_NAME`: Organization name. Used for ssl cert generation.
- `CN_EMAIL`: Email. Used for ssl cert generation.
- `CN_CITY`: City. Used for ssl cert generation.,
- `CN_STATE`: State. Used for ssl cert generation
- `CN_COUNTRY`: Country. Used for ssl cert generation.
- `CN_INSTALL_LDAP`: Default is `false` and will install with `mysql`. If set to `true` LDAP (OpenDJ) will be used as persistence.
- `CN_INSTALL_CONFIG_API`: Default is `true` installing the Config API service.
- `CN_INSTALL_SCIM_SERVER: Default is `true` installing the SCIM service.
- `CN_INSTALL_FIDO2`: Default is `true` installing the FIDO2 service.
- `CN_INSTALL_CLIENT_API`: Default is `true` installing the Client API service.
