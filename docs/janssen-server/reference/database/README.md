---
tags:
  - administration
  - reference
  - database
---

# Overview

Modern systems can work with different DB. Jans is also not exception from this rule. Currently it can work with different DB types and also can work in hybrid environment where application can use the strong power of each DB. This part is based on [jans-orm](https://github.com/JanssenProject/jans/tree/main/jans-orm) layer. It has pluggable architecture which allows to add support for more DB in future.

One of the main target of ORM module is to provide simple lightweight layer to access DB. Also it provides abstractions which hide dependency to specific DB.

## Supported DB

Jans has next persistence modules out-of-the-box:

-  [MySQL](./mysql-config.md)
-  [PostgreSQL](./pgsql-config.md)
-  [Hybrid](./pgsql-config.md). This is virtual DB layer which allows to combine few DB types based on record type.

## Configuration

On a Janssen Server instance 
the type of persistence(DB) used is specified 
in `/etc/jans/conf/jans.properties` using the entry as show below:

```
persistence.type=sql
```

Values for property `persistence.type` is set during the installation and based
on choice of persistence(DB) type, it can be one of these supported values:

- `sql` 
- `hybrid`. 

!!! abstract "Code Connect"
     
     In Janssen Server code base, the list of supported persistence types can be
     found [here](https://github.com/JanssenProject/jans/blob/main/jans-orm/core/src/main/java/io/jans/orm/PersistenceEntryManager.java#L48).

Based on persistent type in use, the corresponding properties file with relevant
configuration properties will be available on Janssen Server instance under
the directory `/etc/jans/conf/`. List of configuration files for these 
persistence types are:

- `jans-sql.properties`
- `jans-hybrid.properties`

The application reloads configuration from these files after start up or on 
files date time modification after initialization.

### Overriding DB Configuration Properties

Configuration properties and the values specified under `.properties` files
discussed in [above](#configuration) section, can be overridden by dynamically
passing them as a JVM parameter or as an environment variable.

#### Cloud-native Installations

For Janssen Server installations using Helm, Docker, or local Kubernetes,
dynamically passing DB configuration properties can be done by
specifying environment variables.

For example:

- Override connection URI for one specific component. For instance,
  `jans-auth` component
  ```shell
  CN_AUTH_JAVA_OPTIONS=-Dconnection.uri=new-mysql-uri
  ```
- Override connection URI globally for all Janssen Server modules
  ```shell
  CN_JAVA_OPTIONS=-Dconnection.uri=new-mysql-uri
  ```
  or by exporting system-level environment variable as below
  ```shell
  CONNECTION_URI=new-mysql-uri
  ```
For a Helm installation you can globally override hte URI in the `values.yaml`

```yaml
global:
  usrEnvs:
    normal:
      CN_JAVA_OPTIONS: -Dconnection.uri=jdbc:postgresql://postgresql.sql.svc.cluster.local:5432/jansdb
```

Or for a specific component like the auth server:

```yaml
auth-server:
  usrEnvs:
    normal:
      CN_JAVA_OPTIONS: -Dconnection.uri=jdbc:postgresql://postgresql.sql.svc.cluster.local:5432/jansdb
```

#### VM Installations

For the Janssen Server installed on a VM, a new parameter or parameters with override
values can be added either at the global level for all modules or specific to
certain modules only. The steps
below show how to override the DB connection URI:

##### Globally For All Modules

Define environment variable at the VM level, such as shown in the example below:

```shell
export CONNECTION_URI=jdbc:postgresql://localhost:5432/jansdb
```

This will affect all Janssen Server modules, where properties like
`connection.uri` or `connection-uri` will be overridden with the new value.

##### Module Specific

Properties can also be added/overridden at the module level as well. To do this,
pass new values either as JVM parameters or as environment variables at
the module level.

Janssen Server module level configuration is stored under `/etc/default/`
directory which contains config files for Janssen modules. For example
`/etc/default/jans-auth` file contains configuration for `jans-auth` module.

- Update the `JAVA_OPTIONS` parameter to add the value of the new connection
  URI against the `connection.uri` parameter.
  For example:
  ```shell
  -Dconnection.uri=jdbc:postgresql://localhost:5432/jansdb`
  ```
- Using the environment variable, export a new variable, as shown in the example
  below:
  ```shell
  export CONNECTION_URI=jdbc:postgresql://localhost:5432/jansdb
  ```
  The exported variable above, `CONNECTION_URI`, will override the value
  provided by `connection.uri` or `connection-uri` parameters.

## Architecture

Jans ORM has modular architecture. The are few layers in this implementation

1. **jans-orm-core**: This is base layer with next functionality:

    - define API methods to work with DB without specific DB dependency

    - provide base implementation with reusable parts needed for DB modules implementation

    - scan beans and work with ORM layer annotations
  
1. **jans-orm-annotation**: Define annotations which instruct ORM about how to work with beans and properties 

1. **jans-orm-filter**: This module provides Filter API. The syntax is very similar to LDAP filters. The ORM layer converts them at runtime to DB query syntax: *SQL/NoSQL*

1. **jans-orm-cdi**: Provides reusable factory which can be used in CDI based projects

1. **jans-orm-standalone**: Provides reusable factory for non CDI based application

1. **jans-orm-ldap, jans-orm-couchbase, jans-orm-sql**: These are DB specific implementations.


## Sample table and ORM API

In order to work with DB ORM layer should know which data (attributes/columns) is needed for application. Most part of this information it takes from DB schema at startup but rest of it developer should define in java bean. Here is typical schema with comments:

```
// Specify that this is Table/objectClass structure
@DataEntry
// Table/objectClass name is `jansPerson`
@ObjectClass(value = "jansPerson")
public class SimpleUser implements Serializable {

    private static final long serialVersionUID = -1634191420188575733L;
  
    // Define entry primary key. i.e a distinguished name, DN
    @DN
    private String dn;

    // Define text attribute with name in DB `uid`.
    // Consistency attribute instruct ORM that it should send query to DB and
    // specify that it should execute it after table index update.
    // This is used with Couchbase DB
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
    // This is convenient for custom attributes
    @AttributesList(name = "name", value = "values", multiValued = "multiValued", sortByName = true)
    protected List<CustomObjectAttribute> customAttributes = new ArrayList<CustomObjectAttribute>();

    ...
```

Main **PersistenceEntryManager** API methods which applications can use:

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

## New DB support and integrations

The ORM developed with perspective to simplify to add new DB support. New ORM implementation requires only few classes/interfaces implementations.

1. Extend `public interface DbOperationService extends PersistenceOperationService` interface to define list of CRUD method which `DbEntryManager` can use.

1. Implement abstract methods `DbEntryManager extends BaseEntryManager<DbOperationService>`. This define DB specific methods to work with DB low layer.

1. Implement `public class DbEntryManagerFactory implements PersistenceEntryManagerFactory`. This CDI `@ApplicationScoped` needed to allow integrate new DB module into application.

1. Put into final jar file `/META-INF/beans.xml` to allow CDI framework automatically find *DbEntryManagerFactory* from previous point.

After this it's enough to put new ORM DB module into application war file or follow customization sections to add jar file to custom jetty classpath.


## DB Schema

The DB schema and beans definitions should match each other. During deployment setup generates schema and indexes based on rules in 
schema [file](https://github.com/JanssenProject/jans/blob/main/jans-linux-setup/jans_setup/schema/jans_schema.json). The data types defined in it setup map to DB SQL types based on default [rules](https://github.com/JanssenProject/jans/blob/main/jans-linux-setup/jans_setup/static/rdbm/ldap_sql_data_type_mapping.json).

It's possible to override default generation rules. For this case there is next [file](https://github.com/JanssenProject/jans/blob/main/jans-linux-setup/jans_setup/static/rdbm/sql_data_types.json).

Default indexes defined in next files: [coubase_index.json](https://github.com/JanssenProject/jans/blob/main/jans-linux-setup/jans_setup/static/couchbase/index.json), [mysql_index.json](https://github.com/JanssenProject/jans/blob/main/jans-linux-setup/jans_setup/static/rdbm/mysql_index.json), [pgsql_index.json](https://github.com/JanssenProject/jans/blob/main/jans-linux-setup/jans_setup/static/rdbm/pgsql_index.json) 
