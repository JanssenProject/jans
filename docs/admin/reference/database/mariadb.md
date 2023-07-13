---
tags:
  - administration
  - reference
  - database
---

# MariaDB ORM persistence layer

The ORM module for MySQL and MariaDB is the same. There is only one difference related to JSON Attributes.
MariaDB allows to use **JSON** keyword during table creation but it's return column type **LONGTEXT** when application request schema. To allow ORM scan schema at startup each **JSON** attribute should has specific `CONSTRAINT`. Example:

```
ALTER TABLE jansPerson
ADD CONSTRAINT customJsonColumn CHECK(JSON_VALID("customJsonColumn"));
```

If ORM find `JSON_VALID` in column definition it works with it as with **JSON** column.

More details about this ORM is in [MySQLthis](.//mysql-config.md) documentation.

