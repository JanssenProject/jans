## Gluu IANA OID Numbers

The OIDs are dot seperate numbers with the number `1.3.6.1.4.1.48710` allocated to Gluu by the IANA. This can be used as the base and expanded by adding dots and numbers.

Gluu uses a hierarchial structure to define the schema elements and allots numbers in the same structure.
```
1.3.6.1.4.1.48710 (Organization OID for Gluu)
1.3.6.1.4.1.48710.0 (Reserved Group for testing and other purposes)
1.3.6.1.4.1.48710.1 (Published Group used in Gluu Server LDAP schema)
1.3.6.1.4.1.48710.1.1 (Syntax definitons)
1.3.6.1.4.1.48710.1.2 (Matching Rule definitions)
1.3.6.1.4.1.48710.1.3 (Attribute definitions)
1.3.6.1.4.1.48710.1.4 (Object Class definitions)
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
