---
tags:
  - administration
  - reference
  - database
---

# Overview

Modern systems can work with different DB. Jans is also not exception from this rule. Currently it can work with different DB types and also can work in hybrid environment where application can use the strong power of each DB. This parts is based on [jans-orm](https://github.com/JanssenProject/jans/tree/main/jans-orm) layer. It has pluggable architecture which allows to add support more DB in future.

One of the main targets of ORM module is to provide simple lightweight layer to access DB. Also it provides abstractions which hide dependency to specific DB.

## Supported DB

Jans has next persistence modules out-of-the-box:
-  [LDAP](.//ldap-config.md)
-  [Couchbase](.//cb-config.md)
-  [Spanner](.//spanner-config.md)
-  [MySQL](.//mysql-config.md)
-  [PostreSQL](.//postgres-config.md)
-  [MariaDB](.//mariadb.md)
-  [Hybrid](.//postgres-config.md). This is virtual DB layer which allows to combine few DB types based on record type.

## Configuration

Type of DB layer is specified in */etc/jans/conf/*:

```
persistence.type=ldap
```

The list of allowed values is *ldap, couchbase, sql, spanner, hybrid*. It's defined in [list](https://github.com/JanssenProject/jans/blob/main/jans-orm/core/src/main/java/io/jans/orm/PersistenceEntryManager.java#L48).

The corresponding list of configuration files for these persistence types are:
- *jans-ldap.properties*
- *jans-couchbase.properties*
- *jans-spanner.properties*
- *jans-sql.properties*
- *jans-hybrid.properties*

These files contains DB specific properties and format of them is specified in the relevant sections of this documentation.

The applications reloads configuration from these files after start up on files date time modifications.

## Architecture

Jans ORM has modular architecture. The are few layers in this implementation

1. *jans-orm-core*: This is base layer with next functionality:

    - define API methods to work with DB without specific DB dependency

    - provide base implementation with reusable parts needed for DB modules implementation

    - scan beans and work with ORM layer annotations
  
1. *jans-orm-annotation*: Define annotations which instruct ORM about how to wrok with beans and properties 

1. *jans-orm-filter*: This module provides Filter API. The syntax is very similar to LDAP filters. The ORM layer converts them at runtime to DB query syntax like: SQL/NoSQL/LDAP

1. *jans-orm-cdi*: Provides reusable factory which can be used in CDI based projects

1. *jans-orm-standalone*: Provides reusable factory for non CDI based application

1. *jans-orm-ldap, jans-orm-couchbase, jans-orm-spanner, jans-orm-sql*: These are DB specific implementations.


## Sample table and ORM API

In order to work with DB ORM should know which data is needed for application. Most part of this information it takes from DB schema at startup but rest of it developer should define in java bean. Here is typical schema with comments:

```
// Specify that this is Table/objectClass structure
@DataEntry
// Table/objectClass name is `jansPerson`
@ObjectClass(value = "jansPerson")
public class SimpleUser implements Serializable {

    private static final long serialVersionUID = -1634191420188575733L;
  
    // Define entry primary key. In LDAP terminology it's DN
    @DN
    private String dn;

    // Define text attribute with name in DB `uid`. Consistency attribute specified to specify that ORM should send query to DB and
    // specify that it should execute it after table index update. This is needed for DB like Couchbase
    @AttributeName(name = "uid", consistency = true)
    private String userId;

    // Define date/time attribute with name in DB `updatedAt`
    @AttributeName
    private Date updatedAt;

    // Define date/time attribute with name in DB `jansCreationTimestamp`
    @AttributeName(name = "jansCreationTimestamp")
    private Date createdAt;

    // Define multivalued text attribute with name in DB `jansPersistentJWT`
    @AttributeName(name = "jansPersistentJWT")
    private String[] oxAuthPersistentJwt;


    // All attributes which not defined above ORM should put into the List<CustomObjectAttribute>
    // This is useful for custom attributes
    @AttributesList(name = "name", value = "values", multiValued = "multiValued", sortByName = true)
    protected List<CustomObjectAttribute> customAttributes = new ArrayList<CustomObjectAttribute>();

    // Specify additional objeClass
    @CustomObjectClass
    private String[] customObjectClasses;

    ...
```

Main *PersistenceEntryManager* API methods which applications can use:

```
    <T> boolean authenticate(String primaryKey, Class<T> entryClass, String password);
    <T> boolean authenticate(String baseDN, Class<T> entryClass, String userName, String password);

    void persist(Object entry);
    void merge(Object entry);

    <T> boolean contains(String primaryKey, Class<T> entryClass);
    <T> boolean contains(String primaryKey, Class<T> entryClass, Filter filter);

    <T> int countEntries(Object entry);
    <T> int countEntries(String primaryKey, Class<T> entryClass, Filter filter);
    <T> int countEntries(String primaryKey, Class<T> entryClass, Filter filter, SearchScope scope);

    <T> T find(Object primaryKey, Class<T> entryClass, String[] ldapReturnAttributes);

    <T> List<T> findEntries(Object entry);
    <T> List<T> findEntries(Object entry, int count);

    <T> List<T> findEntries(String primaryKey, Class<T> entryClass, Filter filter);
    <T> List<T> findEntries(String primaryKey, Class<T> entryClass, Filter filter, int count);
    <T> List<T> findEntries(String primaryKey, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes);
    <T> List<T> findEntries(String primaryKey, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes, int count);
    <T> List<T> findEntries(String primaryKey, Class<T> entryClass, Filter filter, SearchScope scope, String[] ldapReturnAttributes,
                            int start, int count, int chunkSize);
    <T> List<T> findEntries(String primaryKey, Class<T> entryClass, Filter filter, SearchScope scope, String[] ldapReturnAttributes,
                            BatchOperation<T> batchOperation, int start, int count, int chunkSize);

    <T> PagedResult<T> findPagedEntries(String primaryKey, Class<T> entryClass, Filter filter, String[] ldapReturnAttributes, String sortBy,
                                        SortOrder sortOrder, int start, int count, int chunkSize);

	void remove(Object entry);

	<T> void removeByDn(String dn, String[] objectClasses);
	<T> void remove(String primaryKey, Class<T> entryClass);

	<T> int remove(String primaryKey, Class<T> entryClass, Filter filter, int count);
	
	<T> void removeRecursively(String primaryKey, Class<T> entryClass);
	<T> void removeRecursivelyFromDn(String primaryKey, String[] objectClasses);

    boolean hasBranchesSupport(String primaryKey);
    boolean hasExpirationSupport(String primaryKey);

    String getPersistenceType();
    String getPersistenceType(String primaryKey);

    Date decodeTime(String primaryKey, String date);
    String encodeTime(String primaryKey, Date date);

    int getHashCode(Object entry);

    <T> List<AttributeData> exportEntry(String dn, String objectClass);

    <T> void importEntry(String dn, Class<T> entryClass, List<AttributeData> data);
``` 

## New DB support and integration

The ORM developed with perspective to simplify add new DB support. New ORM implementation requires only few classes and interfaces implementation.

1. Extend `public interface NewDbOperationService extends PersistenceOperationService` interface to define list of CRUD method which `DbEntryManager` can use.

1. Implement abstract methods `CouchbaseEntryManager extends BaseEntryManager<CouchbaseOperationService>`. This define DB specifc method to work with DB layer data.

1. Implement `public class DbEntryManagerFactory implements PersistenceEntryManagerFactory`. This CDI `@ApplicationScoped` neede to allow integrate new DB module into application.

1. Put into final jar file `/META-INF/beans.xml` to allow CDI framework automatically find *DbEntryManagerFactory* from previous point.

After this it's enough to put new ORM DB module into final war or follow customization sections to add jar file to custom jetty classpath.


## DB Schema

The DB schema and beans definitions should match each other. During deployment setup generates schema and indexes based on rules in 
schema [file](https://github.com/JanssenProject/jans/blob/main/jans-linux-setup/jans_setup/schema/jans_schema.json). The data types defined in it setup map to DB SQL types based on default [rules](https://github.com/JanssenProject/jans/blob/main/jans-linux-setup/jans_setup/static/rdbm/ldap_sql_data_type_mapping.json).

It's possible to override default generation rules. For this case there is [file](https://github.com/JanssenProject/jans/blob/main/jans-linux-setup/jans_setup/static/rdbm/sql_data_types.json).

Default indexes defined in next files: [coubase_index.json](https://github.com/JanssenProject/jans/blob/main/jans-linux-setup/jans_setup/static/couchbase/index.json), [spanner_index.json](https://github.com/JanssenProject/jans/blob/main/jans-linux-setup/jans_setup/static/rdbm/spanner_index.json), [mysql_index.json](https://github.com/JanssenProject/jans/blob/main/jans-linux-setup/jans_setup/static/rdbm/mysql_index.json), [pgsql_index.json](https://github.com/JanssenProject/jans/blob/main/jans-linux-setup/jans_setup/static/rdbm/pgsql_index.json) 
