---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
---

# Lock Master

## Jans Lock Overview

Lock Master is a Java Weld application that connects ephemeral Cedarlings to the enterprise by 
providing a number of [endpoints](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/main/jans-lock/lock-master.yaml)

## Installation 

Admins can deploy Lock Master as part of Jans Auth Server or as a stanalone web server.

## Configuration 

A list of server-level configuration properties.

## Logs 

Lock Master creates the following logs
* lock_master_config.log
* lock_master_audit.log -- RDBMS option
* lock_master_jwt_status.log 

## CLI / TUI 

Admins can manage Lock Master runtime configuration and see activity using the Jans CLI or TUI.

* Create/Read/Update/Delete Policy Stores
* Total Number of Authz requests per day
* View/Search current Cedarling clients by searching for username
  * View authz activity for this Cedarling client

## OAuth Security

Cedarling should present an SSA during client registration. This will enable Cedarlings to obtain access 
tokens with scopes for OAuth protected Lock Master endpoints.