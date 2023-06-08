---
tags:
  - administration
  - reference
  - database
---

# Overview

Modern systems can work with different DB. Jans is also not exception from this rule. Currently it can work with different DB types and also can work in hybrid environment where application can utilitize the strong power of each DB. This parts is based on [jans-orm](https://github.com/JanssenProject/jans/tree/main/jans-orm) layer. It has pluggable architecture which allows to add support of more DB in future.


** Supported DB **
Jans has next persistence modules out-of-the-box:
~  [LDAP](.//ldap-config.md)
~  [Couchbase](.//cb-config.md)
~  [Spanner](.//spanner-config.md)
~  [MySQL](.//mysql-config.md)
~  [PostreSQL](.//postgres-config.md)
~  [MariaDB](.//mariadb.md)
~  [Hybrid](.//postgres-config.md). This is virtual DB layer which allows to combine few DB types based on record type.

** Configuration **
Type of DB layer plugin is specified in **