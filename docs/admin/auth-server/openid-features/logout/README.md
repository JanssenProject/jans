---
tags:
  - administration
  - auth-server
  - openidc
  - feature
  - logout
---

# Overview

Although Jans Auth Server has a `session_id` for each person who has authenticated,
applications generally have their *own* sessions. Upon logout from an OpenID Provider, ideally all RPs are notified, so they can also their local sessions. The OpenID solution to implement logout is currently described in the [OpenID Connect Front Channel Logout specification](http://openid.net/specs/openid-connect-frontchannel-1_0.html).

In practice, here's how it works:

 - Jans Auth Server `end_session` endpoint returns an HTML page, which contains an iFrame for each application to which the user has authenticated.
 - The iFrame contains a link to each application's respective logout URL.
 - The special HTML page should be loaded in the background and not displayed to the user.
 - The iFrame URLs should be loaded by the browser.
 - Now, upon logout, the user is calling the logout page of each application, the local cookies are cleared, and the user is signed out of all applications.  
