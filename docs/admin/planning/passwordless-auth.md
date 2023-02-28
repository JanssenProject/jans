---
tags:
  - administration
  - planning
---

Auth Server is not proscriptive about how an organization authenticates a
person. If you want to avoid passwords, that's great. Everyone knows
passwords are terrible.

Modern authentication flows use a series of web pages to mitigate sufficient
risk, enabling them to issue an assertion about the subject's identity. In
order to perform authentication, identification is a given. This implies that
all authentication flows normally start with the subject asserting some kind of
identifier: username, email address, phone number, or any other identifier that
uniquely identifies a person. Auth Server is not opinionated about how you do
this. You could prompt the user to enter their identifier. Or for example, you
could ask the user to scan a QR code, and identify a phone identified with a
person.

With identification done, Auth Server can present any number of additional
web pages to establish that identity. These pages can ask for any "factors".
For example, if you want to perform two factor authentication in one step,
you could use a FIDO 2 credential, which combines possession with either
knowledge or biometric. But in practice, you could ask for any one or more
combination of credentials--none of which must include password.

Net-net, "passwordless" is really just marketing jargon. Normally it implies
some kind of risk assessment to optimize user experience. If you can imagine
any such authentication flow, you can implement it in Auth Server. 
