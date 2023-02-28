---
tags:
  - administration
  - planning
  - IDM
  - Identity Management
  - SCIM
---

# Identity Management

In Janssen docs, the term Identity Management or "IDM" is the process of
adding, editing and deleting data in various domain systems. Ideally, identity
data would exist in only one place. But this ideal is currently undesirable.
For performance, data joins, auditing, and many other reasons (for the
foreseeable future) identity data is sprinkled across many systems. So when we
update identity data, how do we keep all our infrastructure up-to-date? This
task has been the realm of IDM platforms, which enable organizations to define
workflows to update identity information, and through the use of "connectors",
to push this data to all disparate databases.

IDM platforms range in size and complexity. An admin could write a Python
script to automate adding new users to a domain. Or large enterprises may
implement a commercial IDM platform, like [Sailpoint](https://sailpoint.com) or
an open source IDM platform like [Evolveum Midpoint](https://evolveum.com).
Or you might have a human IDM--"Hey Bob, we hired a new person. Can you add
her?"

The Janssen platform does not include an IDM component. Fundamentally, Janssen
is a consumer of identity data. One of the most common ways for identity data
to make its way to the Janssen database is via the [SCIM](https://simplecloud.info)
interface. This is an API that has a `/users` endpoint, to which an IDM system
can send updates. For example, the IDM system may `POST` to the `/users`
endpoint to add a new user to the Janssen database, or `DELETE` to the `/users`
endpoint to remove a user.

With that said, sometimes organizations might encode IDM business logic in the
Janssen platform. This is particularly true for consumer-facing applications.
In general, it only works for relatively simple requirements, particularly when
the Janssen platform is the authoritative source of identity data. Using
the various interception scripts, it's possible to send identity data from
the Janssen platform to external systems. For example, let's say an organization
has only two silos of identity data: Jans Auth Server and a MongoDB database
record. In a case like this, when a person registers through a Person
Authn Interception Script or Agama flow, you could call an API which updates
the MongoDB database. It's also possible to implement approval workflows using
UMA. For example, an API might require an UMA access token (i.e. an RPT token),
and obtaining this token may require the consent of two different individuals.

But it is worth remembering that the Janssen Platform was not purpose built for
IDM, and therefore any implementation for such use cases should be tactical.
You may have to build many features yourself. Especially for enterprise
workforce applications, you should seriously consider using off-the-shelf
software before writing too much code in Janssen.
