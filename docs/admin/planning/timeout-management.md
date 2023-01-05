---
tags:
  - administration
  - planning
---

Warning: business expectations regarding timeouts may drive you crazy. There
are several timeout values in Janssen Auth Server. And web application and
mobile apps may have their own timeout thresholds. The holy grail of timeouts
is when everything times out at the same time--IDP and applications. This
is rarely attainable. Normally the application needs to handle gracefully the
case when the IDP session expires first. And if the application session expires
first, it may need to trigger a logout event at the OpenID Provider.

Below is a list of some of the Auth Server configuration properties for timeouts
that you should consider:

1. **sessionIdLifetime**:

1. **serverSessionIdLifetime**:

1. **sessionIdUnusedLifetime**:

1. **sessionIdUnauthenticatedUnusedLifetime**:

1. **spontaneousScopeLifetime**:

1. **refreshTokenLifetime**:

1. **idTokenLifetime**:

1. **accessTokenLifetime**:

1. **cleanServiceInterval**:

1. **dynamicRegistrationExpirationTime**:

1. **refreshTokenExtendLifetimeOnRotation**:

1. **CIBA, UMA and Device Flow**: have their own specific timeouts.
