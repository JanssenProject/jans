---
tags:
  - administration
  - scim
---

# How is SCIM data stored?

SCIM [schema spec](https://datatracker.ietf.org/doc/html/rfc7643) does not use LDAP attribute names but a different naming convention for resource attributes (note this is not the case of custom attributes where the SCIM name used is that of the LDAP attribute).

It is possible to determine if a given LDAP attribute is being mapped to a SCIM attribute. For that you need to check in Jans TUI `Auth-Server >> Attributes` and click on any attributes. Check `Include in SCIM Extension:` is `true` or `false`. Whenever you try to map any LDAP attribute to a SCIM attribute keep it's value `true`.

## Have questions in the meantime?

While this documentation is in progress, you can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
