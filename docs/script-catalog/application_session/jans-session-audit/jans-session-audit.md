---
tags:
  - administration
  - developer
  - script-catalog
---

# Jans Session Audit Script Guide

The Jans Session Audit script is an implementation of the Application Session interception script in Janssen Auth Server. It records configured session lifecycle events and stores audit information in the persistence database.

The script generates audit records containing a JSON-formatted `jansData` field with the event type and selected session attributes.

## Purpose

The script can be used to:

- Record authentication, session update, and session removal events.
- Capture selected session information, such as the session ID, user DN, client ID, and scope.
- Store session audit records for troubleshooting and auditing.

## Source of Script

The Jans Session Audit implementation is available in the Janssen repository:

[Jans Session Audit Python Script](https://github.com/JanssenProject/jans/blob/main/docs/script-catalog/application_session/jans-session-audit/jans-session-audit.py)

## Interface

Jans Session Audit implements the `ApplicationSessionType` interface and uses the `onEvent()` callback to process session events and generate audit records.

For details about the interface and its callback methods, see the [Application Session Script Guide](../application-session.md#interface).


## Configuration

The script can be configured using Jans TUI or Jans CLI.

### Configuration Properties

| Property                           | Description                                                      |
| ---------------------------------- | ---------------------------------------------------------------- |
| `metric_audit_ou_name`             | Organizational unit name used for audit metric records.          |
| `metric_audit_conf_json_file_path` | Absolute path to the audit configuration JSON file.              |
| `log_level`                        | Script logging level, such as `DEBUG`, `INFO`, or `ERROR`.       |

### JSON Configuration

The configuration file contains three properties:

| Property          | Description                                                   |
| ----------------- | ------------------------------------------------------------- |
| `event_types`     | Session event types to record.                              |
| `audit_data`      | Standard session properties to include in the audit record.   |
| `audit_cust_data` | Additional session attributes to include in the audit record. |

#### `event_types`

The supported event types are:

- `AUTHENTICATED`
- `UNAUTHENTICATED`
- `UPDATED`
- `GONE`

#### `audit_data`

The script supports selecting session properties such as:

| Property               | Description                                      |
| ---------------------- | ------------------------------------------------ |
| `userDn`               | User distinguished name.                         |
| `id`                   | Session identifier.                              |
| `outsideSid`           | External session identifier.                     |
| `lastUsedAt`           | Last session activity time.                      |
| `authenticationTime`   | Authentication timestamp.                        |
| `state`                | Session state information.                       |
| `expirationDate`       | Session expiration timestamp.                    |
| `sessionState`         | Session state value.                             |
| `permissionGranted`    | Permission-granted information.                  |
| `permissionGrantedMap` | Map of permission-granted information.           |
| `deviceSecrets`        | Device-related secret information.               |

#### `audit_cust_data`

Additional session attributes can include:

`auth_external_attributes`, `opbs`, `response_type`, `client_id`, `auth_step`, `acr`, `casa_logoUrl`, `remote_ip`, `scope`, `acr_values`, `casa_faviconUrl`, `redirect_uri`, `state`, `casa_prefix`, `casa_contextPath`, and `casa_extraCss`.

Use `sessionAttributes` as the sole entry in `audit_cust_data` to include the available extra session attributes instead of selecting them individually.

## Configuration Examples

The configuration is stored in the JSON file specified by the `metric_audit_conf_json_file_path` script property, for example `/etc/jans/conf/jans-session-audit.json`.

### Audit Authentication and Session Removal Events

For an `AUTHENTICATED` event, the script stores the following fields in the `jansData` audit record:

```json
{
  "event_types": ["AUTHENTICATED", "GONE"],
  "audit_data": ["id", "userDn"],
  "audit_cust_data": []
}
```

For an authenticated session, the generated `jansData` field may contain:

```json
{
  "type": "AUTHENTICATED",
  "id": "example-session-id",
  "userDn": "inum=example-user,ou=people,o=jans"
}
```

### Include Additional Session Attributes

To include the available extra session attributes, use the special `sessionAttributes` value:

```json
{
  "event_types": ["AUTHENTICATED", "GONE"],
  "audit_data": ["id", "userDn"],
  "audit_cust_data": ["sessionAttributes"]
}
```

This configuration records the session identifier, user DN, and available extra session attributes.
