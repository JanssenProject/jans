## Gluu IANA OID Numbers

The OIDs are dot seperate numbers with the number `1.3.6.1.4.1.48710` allocated to Gluu by the IANA. This can be used as the base and expanded by adding dots and numbers.

Gluu uses a hierarchial structure to define the schema elements and allots numbers in the same structure.
```
1.3.6.1.4.1.4870
├── .0-Reserved
└── .1-Published
    ├── .1-Syntax
    ├── .2-MatchingRule
    ├── .3-Attribute
    └── .4-ObjectClass

1.3.6.1.4.1.48710 (Organization OID for Gluu)
1.3.6.1.4.1.48710.0 (Reserved Group for testing and other purposes)
1.3.6.1.4.1.48710.1 (Published Group used in Gluu Server LDAP schema)
1.3.6.1.4.1.48710.1.1 (Syntax definitons)
1.3.6.1.4.1.48710.1.2 (Matching Rule definitions)
1.3.6.1.4.1.48710.1.3 (Attribute definitions)
1.3.6.1.4.1.48710.1.4 (Object Class definitions)
1.3.6.1.4.1.48710.1.10 (Customer Allocations)

```

OpenLDAP Schema definition rules give a way to abstract this structure into aliases that are easier to be used in definitions. The following aliases are defined in `gluu.schema`


```
objectIdentifier oxOrgOID      1.3.6.1.4.1.48710
objectIdentifier oxReserved    oxOrgOID:0
objectIdentifier oxPublished   oxOrgOID:1
objectIdentifier oxSyntax      oxPublished:1
objectIdentifier oxMatchRules  oxPublished:2
objectIdentifier oxAttribute   oxPublished:3
objectIdentifier oxObjectClass oxPublished:4
```

So any new definition could be added by specifying the alias followed by a number.

```
attributetype ( oxAttribute:1 NAME ( 'oxAssociatedClient' 'associatedClient' )
...
... )

objectclass ( oxObjectClass:999 NAME 'myObjectClass' SUP top
...
... )
```

### Note for Schema Editors

* While defining the schema, use a `colon` followed by the number NOT a `dot`
* The file `custom.schema` starts with the `oxAttribute:1001` and `oxObjectClass:101`. Kindly keep this in mind when expanding `gluu.schema` so as not to run into the custom schema number space.
* In order to keep the OpenLDAP and OpenDJ schema files it sync, after a definition is added or removed from the gluu.schema file, do the following

    ```
    cd <parent dir of community-edition-setup>
    wget https://raw.githubusercontent.com/ludomp/opendj-utils/master/schema-convert.py
    python schema-convert.py -o gluu.ldif community-edition-setup/static/openldap/gluu.schema
    ```
    Compare the gluu.ldif file with the `community-edition-setup/static/opendj/deprecated/101-ox.ldif` and copy the headers from 101-ox.ldif to the new gluu.ldif file. Now rename `gluu.ldif` to `101-ox.ldif` and replace the `101-ox.ldif` file inside the `community-edition-setup` with this new file, commit and push.
