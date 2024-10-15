---
tags:
  - administration
  - user management
---

# Local User Management

In this document we will cover managing people in the Jans Server's LDAP Directory, Jans CLI / TUI and using SCIM.

=== "Manage Data using DB Browser"

    You can manage your Janssen Server data using external tool provided by specific DB. For example, to manage PostgreSQL data, Jump into [Manage RDBMS Data](#manage-Data-in-RDBMS) section for guidelines.

=== "User Management using SCIM"

    SCIM allows many ways to manage users data. Jump into the [SCIM User Management](../config-guide/scim-config/user-config.md) for guidelines of SCIM operations. To know how SCIM works in Janssen Server, read more from [here](../scim/README.md).
    
=== "Manage External Data Sources Using Link"

    Janssen Server allows connecting external data sources using Jans Link. Syncing people and attributes from a backend server speeds up authentication transactions. It is possible to perform attribute transformations, changing the name of attributes, or even using an interception script to change the values. Transformations are stored locally in Janssen Server. Read out [Link Guide](../link/README.md) to know more details on it.
    

## Manage Data in RDBMS

If you choose any of RDBMS like mysql, or postgreSQL, you can explore your jans server database using any tool that are supported by the regarding RDBMS. Let's see how we can connect PostgreSQL DB from jans server to your local PC.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
