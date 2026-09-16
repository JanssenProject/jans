---
tags:
 - administration
 - auth-server
 - session
---

# Session Management

A session is a reference identifier on the Jans Auth Server that connects to a
person's authentication state. During an authentication workflow, Auth Server writes a cookie with the `session_id` in the person's browser. OpenID Native
SSO defines a way for mobile apps from the same vendor to use the iOS or Android
protected secret storage to store the `session_id`. By correlating the session,
the IDP can return an identity assertion (id_token) to a client without needing
to re-authenticate the person. SSO ensues.

For example, let's say a person uses a browser to navigate to the website of
Relying Party (RP1), which redirects to Jans Auth Server for authentication.
Once the person is authenticated, the OP creates a `session_id` cookie, sets the
state to `authenticated`, and places it in the cache. If the person mavigates
their browser to the website of RP2, it redirects to the OP for authentication; since the `session_id` detected via the cookie is already authenticated, the OP
authenticates the person automatically for RP2 (without an authentication
prompt).

Jans Auth Server stores user session data in its cache. Auth Server performance
retrieving the session will vary depending on whether the session is stored in memory, Redis, Memcached or the database, as controlled by the
`cacheProviderType` Auth Server configuration property.

The Auth Server session can have one of two states:

- `unauthenticated` - a browser that has started, but not completed an authentication workflow.
- `authenticated` - when a person has successfully authenticated

The following Auth Server configuration properties are related to sessions:

- **sessionIdCookieLifetime** - The lifetime of `session_id` cookie in seconds. If 0 or -1 then expiration is not set. session_id cookie expires when browser session ends. Default value is `86400`.
- **sessionIdLifetime** - lifetime of the OP session in seconds (server side object). If not set, falls back to `session_id` cookie expiration set by `sessionIdCookieLifetime` configuration property.
- **sessionIdUnusedLifetime** - unused OP session lifetime in seconds. If an OP session is not used for a given amount of time, the OP session is removed.
  Default value is `86400`.
- **sessionIdUnauthenticatedUnusedLifetime** - lifetime in seconds of `unauthenticated` OP session. This determines how long the user can be on the login page while unauthenticated. Default value is `120`.
- **sessionIdRequestParameterEnabled** - Boolean value specifying whether to enable `session_id` HTTP request parameter. Default value is `False`.
- **sessionIdPersistOnPromptNone** - specifies whether to persist or update the session object with data if `prompt=none`. Default value is `True`.
- **invalidateSessionCookiesAfterAuthorizationFlow** - this is special property which specifies whether to invalidate `session_id` and `consent_session_id` cookies right after successful or unsuccessful authorization.
- **changeSessionIdOnAuthentication** - Using a different session after the user authenticates improves security. The default value is `True`.
- **sessionIdPersistInCache** - If True, sessions are stored according to `cacheProviderType`. Otherwise, sessions are persisted in the database.
 Default value is `False`.
- **sessionIdPersistInCache** Default value is `False`.
- **cookieSameSite** - `SameSite` attribute value (`None`, `Lax` or `Strict`) set on all cookies created by the OP (`session_id`, `uma_session_id`, `session_state`, `opbs`, `current_sessions`, `consent_session_id`, `rp_origin_id`). Default value is `None`. See [SameSite attribute](#samesite-attribute) below before changing it.

## SameSite attribute

`cookieSameSite` controls the `SameSite` attribute of all cookies the OP
creates, including the session-related ones. It defaults to `None`, which for
SameSite-aware, spec-compliant clients preserves the cross-site SSO behavior
these cookies already relied on before this attribute existed. It is not
identical to sending no `SameSite` attribute at all: a documented set of older
or non-compliant clients (e.g. Safari on macOS 10.14/iOS 12, Chrome/Chromium
&lt;= 67, some embedded WebViews, UC Browser &lt; 12.13.2) mishandle an
explicit `SameSite=None` value and may reject or drop the cookie even though
they accepted the same cookie with no `SameSite` attribute. Deployments that
must still support such legacy clients should account for this before
upgrading.

`CookieService` sets `Secure` on all of these cookies, but `HttpOnly` only on
`session_id`, `uma_session_id`, `current_sessions`, `consent_session_id` and
`rp_origin_id` - `session_state` and `opbs` intentionally omit `HttpOnly` so
OP-hosted iframe JavaScript can read them for OIDC Session Management. Neither
`Secure` (HTTPS-only transmission) nor `HttpOnly` (blocks JS access) is a CSRF
defense; `SameSite=None` adds no CSRF hardening of its own, it simply
preserves every cross-site SSO flow that Janssen supports today.

Changing the value tightens CSRF defense-in-depth but can break SSO for RPs
hosted on a different site (eTLD+1) than the OP, which is the common Janssen
deployment topology:

- **`Lax`** breaks:
 - Silent authentication / silent token renewal via a hidden cross-site
   iframe (`prompt=none`, e.g. `oidc-client-js` `signinSilent()`). Browsers
   do not attach `Lax` cookies to cross-site iframe navigations, so the OP
   can't see the existing session and returns `login_required` (or
   re-prompts) instead of silently confirming it.
 - Authorization requests submitted to `/authorize` via a cross-site
   auto-submitted HTML form `POST` instead of a `GET` redirect - `Lax`
   excludes cross-site `POST`, so the session isn't recognized and the user
   is forced to re-authenticate.
 - Any deployment that embeds OP-hosted login/consent UI in a cross-site
   iframe.
- **`Strict`** breaks everything `Lax` breaks, plus the core SSO redirect
  itself: a normal top-level, cross-site `GET` redirect from an RP to
  `/authorize` no longer carries the session cookie at all, since RP and OP
  are almost always different sites. Every login looks like a first-time
  visit, effectively disabling SSO for any RP not on the same site as the OP.
  RP-initiated logout redirects to `/end_session` lose the session cookie
  too, so session termination has to rely solely on `id_token_hint`/`sid`
  instead of the cookie.

Only set `cookieSameSite` to `Lax` or `Strict` after confirming none
of your RPs rely on the patterns above.

For both `unused` properties, Jans Auth Server calculates this period as `currentUnusedPeriod = now - session.lastUsedAt`. So for OP session with states:

- `unauthenticated` - if `currentUnusedPeriod` >= `sessionIdUnauthenticatedUnusedLifetime`, then the session object is removed.
- `authenticated` - if `currentUnusedPeriod` >= `sessionIdUnusedLifetime`, then the session object is removed.

Jans Auth Server updates `lastUsedAt` property of the session object:

- During creation
- For each Auth Server authentication attempt (regardless of success)

## Killing Sessions

The [End Session endpoint](../endpoints/end-session.md) (`/end_session`)
is where the user can end their own session. See [OpenID Logout](../openid-features/logout/README.md) for more information.

To end another person's session, Jans Auth Server supports [Global Token Revocation Endpoint](../endpoints/global-token-revocation.md) (`/global-token-revocation`').

## Session Event Interception Scripts

It is possible to add custom business logic as Jans Auth Server detects
session events, see:

* [Application Session](../../../script-catalog/application_session/application-session.md)
* [End Session](../../../script-catalog/end_session/end-session.md)


## Session data structure in Persistence

### MySQL
All session information is saved in this table : https://github.com/JanssenProject/jans/blob/main/docs/admin/reference/database/mysql-schema.md#janssessid

Follow [this link](../../config-guide/auth-server-config/session-management.md) for session management with configuration tools.


## FAQ

### How can we force the user to log out if the user is idle on the RP for 4 hours?

The OP doesn't know anything about end-user activity on the RP. Therefore, the RP has to track activity internally, and when the inactivity period is reached (in this case, 4 hours) the RP should perform front-channel logout.

### How can we force the user to log out if the browser is closed?

Setting `sessionIdLifetime` to `-1` value sets the `session_id` cookie value to `expires=session`, and sets the OP session object to not have an expiration time. Most browsers clear cookies with `expires=session` when the browser is closed, removing the session object at that time. Javascript may be necessary to override
undesirable default browser behavior.

### Can we have a single session across multiple browsers?

Unfortunately, each browser has its own session cookies, and therefore its own sessions.
