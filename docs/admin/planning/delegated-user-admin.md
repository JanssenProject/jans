---
tags:
  - administration
  - planning
---

Delegated user administration is normally implemented via a website
where a privileged user can search / add / edit / delete people in
their organization. You will not be surprised to hear that this is an
identity management use case that Janssen Auth Server does not provide
out of the box.

But if you wanted to build a web site that performed this function,
you could use the SCIM protocol to do so. In your application, you
could read the user claims to find out the organization, role, or
other information about the person managing data. Then your web application
could use this information to make filters when making
SCIM calls. Or another handy trick is to put the person's role information
in an OAuth access token (using the Update Token script), and then use the
SCIM interception script to add the respective filters.
