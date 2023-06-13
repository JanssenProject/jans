---
tags:
  - administration
  - auth-server
  - openidc
  - feature
  - logout
  - front channel logout
  - back channel logout
  - RP initiated logout
---

# Logout

Janssen Server supports various end-user logout scenrios by supporting OpenID Connect specifications. Mechanisms 
defined in OpenID Connect specifications of [RP-Initiated Logout](https://openid.net/specs/openid-connect-rpinitiated-1_0.html), 
[Front-Channel Logout](https://openid.net/specs/openid-connect-frontchannel-1_0.html) and 
[Back-Channel Logout](https://openid.net/specs/openid-connect-backchannel-1_0.html) can be leveraged by 
applications(RPs) to implement end-user logout. Simpler logout scenarios can be fulfiled by using features of one of the 
specifications (just using RP initiated logout for example), but for more complex scenarios like single sign-out,
features from multiple specifications may have to be combined. 

## RP-Initiated Logout

### Configuration Properties

## Front-Channel Logout

Although Jans Auth Server has a `session_id` for each person who has authenticated,
applications generally have their *own* sessions. Upon logout from an OpenID Provider, ideally all RPs are notified,
so they can also their local sessions. The OpenID solution to implement logout is currently described in
the [OpenID Connect Front Channel Logout specification](http://openid.net/specs/openid-connect-frontchannel-1_0.html).

In practice, here's how it works:

- Jans Auth Server `end_session` endpoint returns an HTML page, which contains an iFrame for each application to
  which the user has authenticated.
- The iFrame contains a link to each application's respective logout URL.
- The special HTML page should be loaded in the background and not displayed to the user.
- The iFrame URLs should be loaded by the browser.
- Now, upon logout, the user is calling the logout page of each application, the local cookies are cleared, and the user is signed out of all applications.

### Configuration Properties

## Back-Channel Logout

### Configuration Properties