---
tags:
  - components
  - planning
  - architecture
---

This page has a brief description of the major components of a Janssen
deployment.


1. **Auth Server**: This component is the OAuth Authorization Server, the OpenID
Connect Provider, the UMA Authorization Server--this is the main Internet facing
component of Janssen. It's the service that returns tokens, JWT's and identity
assertions. This service must be Internet facing.

1. **Database**: Like most IAM platforms, Janssen requires some kind of
persistence service to store configuration and other entity data (client,
person, scope, attribute, FIDO device, etc.) As different databases are
good for different deployments, Janssen supports a number of options:
OpenDJ, MySQL, Postgres, Couchbase, Google Spanner, and Amazon Aurora. Other
databases may be added in the future.

1. **Cache**: Getting data from a disk is still the slowest part of any
web platform. If you want higher transaction speeds, one strategy is to
use a memory cache instead of the disk (i.e. database). Janssen was designed
to store short lived objects in the cache, like the `code` in the OpenID code
flow (which is only used one time) or access tokens, which only live for a few
minutes. Currently Janssen has three options for cache: `in-memory`, which is
suitable only for one node VM deploymetns; `redis` which is probably your best
option; and `memcached` which you should use if a Redis cache service is not
available (and tends to have more cache misses under high volume).

1. **Key Management** Janssen does a lot of cryptographic signing and
encryption. Where you store the private keys has an impact on the security of
your Janssen platform. For cloud deployments, many providers are providing
key storage as a service. You could also use the file system or an HSM.

1. **FIDO2**:  This component provides the server side endpoints to enroll and
validate devices that use FIDO. It provides both FIDO U2F (register,
authenticate) and FIDO 2 (attestation, assertion) endpoints. This service must
be internet facing.

1. **Config API**: The API to configure the auth-server and other components is
consolidated in this component. This service should not be Internet-facing.

1. **SCIM**: [SCIM](http://www.simplecloud.info/) is JSON/REST API to manage
user data. Use it to add, edit and update user information. This service should
not be Internet facing.

1. **CLI**: While you can use `curl` to call the Config API, CLI is a command
line tool that provides a simple single line options for configuration. In the
background, it is just calling the Config API. To authenticate, you'll use
the OAuth Device flow. The CLI need not be on the same server as any of the
components (you can run it from your desktop). But you will need network
connectivity to the Config API and the Auth Server.

1. **TUI**: An menu-driven interactive tool for configuration, the "TUI" or
"text user interface" might resemble an 90's BIOS configuration, but it gets
the job done without the need for a web browser. Like the CLI, you can run
it from anywhere, but need connectivity to the Config API and Auth Server.
The TUI writes a "CLI log"--the one liner you could have executed to do whatever
you just did in the interface. This will help you if you want to script stuff
later on.

1. **Jans Core**: This library has code that is shared across several janssen
projects. You will most likely need this project when you build other Janssen
components.

1. **Jans ORM**: This is the library for persistence and caching implementations
in Janssen. Currently, LDAP and Couchbase are supported. RDBMS is coming soon.

1. **Jans Eleven**: Auth Server can use the Jans Eleven REST API for key
operations, which in turns uses PKCS11 interface of an HSM. Basically, it's a
strategy to share an HSM over a network.

1. **Agama**: The Agama module offers an alternative way to build authentication
flows in Janssen Server. With Agama, flows are coded in a DSL (domain specific
language) designed for the sole purpose of writing web flows.

1. **Setup**: Configuring a Janssen Auth Server platform is complicated. How
do you generate the keys and certificates? How do you generate the minimal
data set to start your system. The setup component helps you bootstrap a minimal
system.