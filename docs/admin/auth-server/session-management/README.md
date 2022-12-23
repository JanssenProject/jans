---
tags:
  - administration
  - auth-server
  - session
---

# Overview

A session is a reference identifier on the Jans Auth Server that connect to a
person's authentication state. Most commonly, the session is stored in
the person's web browser, in the `session_id` cookie. OpenID Native SSO also
defines a way for mobile apps from the same vendor to use the iOS or Android
protected secret storage to store a session. By correlating the session, the IDP
can return an identity assertion (id_token) to a client without needing to
authenticate the person. Thus sessions enable SSO.

For example, let's say a person uses a browser to navigate to the website of
Relying Party (RP1), which redirects to Jans Auth Server for authentication.
Once the person is authenticated, the OP creates a `session_id` cookie, sets the
state to `authenticated`, and places it in the cache. If the user hits RP2,
it will redirect the user to the OP for authentication, and since the session is
already authenticated, the OP authenticates the user automatically for RP2
(without an authentication prompt).  

Jans Auth Server stores user session data in its cache, whether it's in-memory,
redis, memcached or the databse, depending on the `cacheProviderType`
configuration property.

The OP session can have one of two states:

- `unauthenticated` - when the end-user reaches the OP but has not yet authenticated, a session object is created and put in the cache with the `unauthenticated` state.  
- `authenticated` - when the user has successfully authenticated at the OP.


- **sessionIdLifetime** - lifetime of the OP session in seconds. It sets both the -
- **session_id** cookie expiration property as well as the OP session object expiration (if
- **serverSessionIdLifetime** is not set or equals `0` which is default behavior) in the persistence. It's a global property for sessions. Starting in version `4.1`, it is possible to set value to 0 or -1, which means that expiration is not set (not available in `3.1.3` or earlier except `2.4.4`). In this case, the `session_id` cookie expiration value is set to the `session` value, which means it's valid until the browser session ends.
- **serverSessionIdLifetime** - dedicated property to control lifetime of the server side OP session object in seconds. Overrides `sessionIdLifetime`. By default value is 0, so object lifetime equals `sessionIdLifetime` (which sets both cookie and object expiration). It can be useful if goal is to keep different values for client cookie and server object.
- **sessionIdUnusedLifetime** - unused OP session lifetime in seconds (set by default to 1 day). If an OP session is not used for a given amount of time, the OP session is removed.
- **sessionIdUnauthenticatedUnusedLifetime** - lifetime in seconds of `unauthenticated` OP session. This determines how long the user can be on the login page while unauthenticated.
- **sessionIdRequestParameterEnabled** - Boolean value specifying whether to enable session_id HTTP request parameter. Default value is `false` (since 4.2).
- **sessionIdPersistOnPromptNone** - specifies whether to persist or update the session object with data if `prompt=none`. Default value is `true`, so session is persisted by default.
- **invalidateSessionCookiesAfterAuthorizationFlow** - this is special property which specifies whether to invalidate `session_id` and `consent_session_id` cookies right after successful or unsuccessful authorization.

For both `unused` properties, Jans Auth Server calculates this period as `currentUnusedPeriod = now - session.lastUsedAt`. So for OP session with states:

- `unauthenticated` - if `currentUnusedPeriod` >= `sessionIdUnauthenticatedUnusedLifetime`, then the session object is removed.
- `authenticated` - if `currentUnusedPeriod` >= `sessionIdUnusedLifetime`, then the session object is removed.

Jans Auth Server updates `lastUsedAt` property of the session object:

- initially, it is set during creation
- it is updated during each authentication attempt (whether successful or not successful)

It is important to note that the OP session `lastUsedAt` property is not updated
during RP usage.

## Logout

An application may also store its *own* session for the user. Upon logout from the OP, all RPs need to be notified so local sessions can also be cleared/ended. The best way to handle this currently is through "front-channel logout", as described in the [OpenID Connect Front Channel Logout specification](http://openid.net/specs/openid-connect-frontchannel-1_0.html).

In practice, here's how it works:

 - Jans Auth Server `end_session` endpoint returns an HTML page, which contains an iFrame for each application to which the user has authenticated.
 - The iFrame contains a link to each application's respective logout URL.
 - The special HTML page should be loaded in the background and not displayed to the user.
 - The iFrame URLs should be loaded by the browser.
 - Now, upon logout, the user is calling the logout page of each application, the local cookies are cleared, and the user is signed out of all applications.  

Learn more about the flow for logout across many applications in the
[logout docs](../openid-features/logout/README.md).  

## Session Revocation

Note, the [End Session endpoint](../endpoints/end-session.md) (`/end_session`) is where the user can end their own session. To end another person's session, you need to use the [Session
Revocation Endpoint](../endpoints/session-revocation.md) (`/revoke_session`).

## Session Event Interception Script

It is possible to add custom business logic as Jans Auth Server detects
different session events. See:

  * [Application Session](../../../developer/scripts/application-session.md)
  * [End Session](../../../developer/scripts/end-session.md)

## FAQ

### How can we force the user to log out if the user is idle on the RP for 4 hours?

The OP doesn't know anything about end-user activity on the RP. Therefore, the RP has to track activity internally, and when the inactivity period is reached (in this case, 4 hours) the RP has to perform front-channel logout.

### How can we force the user to log out if the browser is closed?

Setting `sessionIdLifetime` to `-1` value sets the `session_id` cookie value to `expires=session`, and sets the OP session object to not have an expiration time. Most browsers clear cookies with `expires=session` when the browser is closed, removing the session object at that time.
