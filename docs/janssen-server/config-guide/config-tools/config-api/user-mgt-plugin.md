---
tags:
  - administration
  - tools
  - config-api
  - plugins
  - user-mgt-plugin
---

# User Management — Role/Permission Authorization

## Overview
Two layers protect user-mgt endpoints:
1. **`Authorization Filter`** — standard OAuth2 scope check.
2. **`User Filter`** — additional, User management plugin specific role/scope check.

## Prerequisite
- **`userRolePermissionValidationEnabled`**:  Runs only if `userRolePermissionValidationEnabled` Config API attribute is set to `true`.
- **Mandatory header**: `User-inum` must be present in `httpHeaders`.
  Missing → `400 Bad Request` — `"Header attribute 'User-inum' missing"`.

## Exempting a machine client

The `User Filter` assumes a human behind the request. A client using the
`client_credentials` grant has no user, so it can satisfy neither the
`User-inum` header nor the role lookup behind it. Automation such as the
Terraform provider is therefore rejected while the filter is on.

List such clients in the `userRolePermissionExcludedClients` Config API
attribute, by client ID:

```json
"userRolePermissionExcludedClients": ["1800.3d29d884-e56b-47ac-83ab-b37942b83a89"]
```

The list is empty by default. The client ID is read from the token
introspection response, never from a request header, so a client cannot exempt
itself. The exemption applies only when the token carries no user; a token
issued for a user goes through the full role check even when its client is
listed. An exempt client is authorized by its granted OAuth scopes alone, so
grant it only the user scopes it needs and do not share it with other
integrations.

## Endpoint → required OAuth scopes 
| Endpoint | Scopes (any of) |
|---|---|
| `GET /user` | `user_read`, `user_write`, `user_admin`, `super_admin_read` |
| `GET /user/{inum}` | same as above |
| `POST /user` | `user_write`, `user_admin`, `super_admin_write` |
| `PUT /user` | `user_write`, `user_admin`, `super_admin_write` |
| `PATCH /user/{inum}` | `user_write`, `user_admin`, `super_admin_write` |
| `DELETE /user/{inum}` | `user_delete`, `user_admin`, `super_admin_delete` |

## Example request
```http
GET /mgt/v1/user/eb2ee139-4a55-4551-a83a-6ac0fa8b0e3f HTTP/1.1
Host: api.example.com
Authorization: Bearer <access_token>
User-inum: eb2ee139-4a55-4551-a83a-6ac0fa8b0e3f
Accept: application/json
```

**Missing `User-inum` header:**
```http
HTTP/1.1 400 Bad Request
WWW-Authenticate: Bearer

Header attribute `User-inum` missing
```

**Insufficient scopes:**
```http
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer

Insufficient scopes!!! Required scope: [user_write], token scopes: [user_read]
```

**Non-admin accessing another user's record:**
```http
HTTP/1.1 400 Bad Request

User{<loggedInUserInum>} does not have 'admin' role to fetch/modify user{<inumPathVariable>}
```