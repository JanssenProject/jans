---
tags:
  - administration
  - scim
  - custom-attributes
---

##  Custom Attributes

[RFC 7643](https://datatracker.ietf.org/doc/html/rfc7643) defines the schema for resource types in SCIM (see section 3.3). In other words, it defines structures in terms of attributes to represent users and groups as well as attribute types, mutability, cardinality, and so on.

Although the schema covers many attributes one might think of, at times you will need to add your own attributes for specific needs. This is where user extensions pitch in, they allow you to create custom attributes for SCIM. To do so, you will have to:

* Add an attribute to LDAP schema

* Include the new attribute in an LDAP's objectclass such as jansPerson

* Register and activate your new attribute through **Jans TUI**.

Please visit this [page](https://docs.jans.io/head/admin/config-guide/attribute-configuration/) for a more detailed explanation. When registering the attribute in the **TUI**, please ensure you have set the `Include in SCIM Extension` parameter to `true`.

![attribute](https://github.com/JanssenProject/jans/assets/43112579/61d0aff6-75fa-4e6b-8db6-2eeb3332cfe5)

Once you submit this form, your attribute will be part of the `User Extension`. You can verify this by inspecting the `Schema` endpoint:

```
https://<host-name>/jans-scim/restv1/v2/Schemas/urn:ietf:params:scim:schemas:extension:gluu:2.0:User
```

![output-json](https://github.com/JanssenProject/jans/assets/43112579/41804347-4084-4bb4-8bc5-05fc220ae394)

In the JSON response, your new added attribute will appear.

To customize the URI associated to the extension (whose default value is `urn:ietf:params:scim:schemas:extension:gluu:2.0:User`
), you can use TUI:

* Navigate to `SCIM` using Jans TUI `/opt/jans/jans-cli/jans_cli_tui.py`
* Locate the Scim properties section
* Set a value in the field `User Extension Schema URI`
* Save the changes

![scim-extention](https://github.com/JanssenProject/jans/assets/43112579/fb5b9d5c-8b17-4be0-af6c-d36389de82d2)








