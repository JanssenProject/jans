---
tags:
  - administration
  - planning
  - localization
  - international
  - language
---

# Customization and Localization

You can customize just about everything in the Janssen Platform.  That's
pretty much the point of running your own identity platform. This includes
the look and feel of the web pages, the language of the user-facing
content, the behavior of endpoints, error messages, and more.

## Behavior of endpoints

Almost every endpoint has an interception script, which enables you to implement
custom business logic. For a more general discussion of the extensibility of the
Janssen platform, see the [Developer Guide](../developer/README.md).

## Web content

There are several front channel flows in Auth Server (front channel = web
browser):

1. OpenID User Authentication
1. OpenID Consent
1. Post-Authentication
1. UMA Claims Gathering

Each of these scripts contains a method which is similar to `getPageForStep`
which fetches the respective [Facelets](https://en.wikipedia.org/wiki/Facelets)
`xhtml` file for that step. Facelets is very friendly to designers, as
it's possible to mix content such as regular HTML with JSF components.
Thus Facelets pages can have both static and **dynamic content** and leverage
all your "normal" web design tools like CSS and JavaScript. Facelets also
supports templates. Each `.xhtml` page has access to the authentication context,
and you can insert data from the respective interception script. Also, each
`.xhtml` page is unique, and has a built in state identifier, preventing Auth
Server from responding to any page which it did not render. For more information,
see the [Developer Guide](../developer/README.md).

Note, in the `getPageForStep` method, you can have a conditional statement
to return different pages based either on the detected or requested language
of the subject.

## Error messages

You can change the descriptions for the various front-channel Auth Server
error messages, for example, what would Auth Server return for a bad
request to the Authorization Endpoint. Of course you can also customize the web
server error messages, like 404, by configuring your web server.  Auth Server
should **never** return a stackTrace.

## Localization for OpenID Connect client metadata

Because client information may be displayed via the OpenID protocol, language
support is needed at the client level. For example, the client name,
description, terms of service URI, or logo may differ based on the end user's
language.

[Section 2.1 of OpenID Connect Client Registration, Metadata Languages and Scripts](https://openid.net/specs/openid-connect-registration-1_0.html#LanguagesAndScripts) states:

>> Human-readable Client Metadata values and Client Metadata values that
>> reference human-readable values MAY be represented in multiple languages and
>> scripts. For example, values such as client_name, tos_uri, policy_uri,
>> logo_uri, and client_uri might have multiple locale-specific values in some
>> Client registrations.

To specify the languages and scripts, [BCP47](https://www.rfc-editor.org/rfc/rfc5646)
language tags are added to Client Metadata member names, delimited by a
`#` character. For example, the value for `tos_uri#ja-Hani-JP` would direct the
subject to the terms of service agreement in Japanese.

Janssen does not [yet](https://github.com/JanssenProject/jans/issues/2776)
support languages syntax for OpenID user claims, as
described in [OpenID Core Section 5.2, Claims Languages and Scripts](https://openid.net/specs/openid-connect-core-1_0.html#ClaimsLanguagesAndScripts).
