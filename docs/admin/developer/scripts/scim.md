---
tags:
  - administration
  - developer
  - scripts
---

# SCIM Guide

## Overview

SCIM script allows you to execute custom logic when certain SCIM API operations are invoked.

With SCIM scripts, custom business logic can be executed when several of the SCIM API operations are invoked. This is useful in many situations, for example:

- Trigger notifications to external systems when certain resources are deleted
- Alter the actual responses the service produces
- Attach additional information to resources when they are created or updated
- Implement a fine-grained level of access to resources so different callers have permission to handle only a restricted number of resources

### Notes:

In this document, the term resources refer to those "entities" the service can manage, for instance, users or groups
The term operation refers to any SCIM functionality accessible through its HTTP endpoints
Basic development skills are assumed. Some grasp of Java and Python are required as well as understanding of the SCIM protocol.


## Interface

### Methods

**API Overview**

Custom scripts adhere to a simple API (ie. a well-defined collection of methods/routines) that is described in the following. It is advised to check the dummy script provided [here](https://github.com/IdentityMethods-UwarajWaman/jans/blob/docs-interception-script-updates/docs/script-catalog/scim/sample-script/SampleScript.py) as you read this section.

**Scripts' config properties**

All methods contain a `configurationAttributes` parameter, this gives access to the configuration properties of the script itself. This is a relevant aspect of Gluu scripts: they are all parameterizable!.

`configurationAttributes` is a `java.util.Map<String, SimpleCustomProperty>` and [here](https://github.com/JanssenProject/jans/blob/6c1caa1c4c92d28571f8589cd701e6885d4d85ef/jans-core/util/src/main/java/io/jans/model/SimpleCustomProperty.java) is how `SimpleCustomProperty` looks.

**Basic Methods**

These are methods not related to SCIM operations but still play key roles:
|Method Name|Description|Return Value|
|:---|:---|:---|
|`init`|Called when the (SCIM) service starts and every time the script properties or code changes|A boolean value describing success or failure|
|`destroy`|Called every time the script properties or code changes (called before `init`)|A boolean value describing success or failure|
|`getApiVersion`|Determines what methods are effectively called when SCIM endpoints are invoked|A positive integer|

**Pre-resource Modification**

They are called when the resource is about to be persisted. The second parameter in these methods hold the object that will be persisted to permanent storage, thus any change or manipulation upon the object will be reflected in the underlying database (as well as in the output of the SCIM operation call itself).

These methods are called regardless of the API version used. Names are self explanatory:

|Methods|2nd param|2nd param Class/Link|
|:---|:---|:---|
|`createUser`, `updateUser`, `deleteUser`|`user`|[ScimCustomPerson](https://github.com/JanssenProject/jans/blob/1753d430037f623cfc11b99ceaa5d172676d9ecb/jans-scim/model/src/main/java/io/jans/scim/model/scim/ScimCustomPerson.java)|
|`createGroup`, `updateGroup`, `deleteGroup`|`group`|[GluuGroup](https://github.com/JanssenProject/jans/blob/6c1caa1c4c92d28571f8589cd701e6885d4d85ef/jans-scim/service/src/main/java/io/jans/scim/model/GluuGroup.java)|

Pre-resource modification methods return a boolean. A `False` value aborts the corresponding SCIM operation and a 500 error is returned. The same applies if the method execution crashes at runtime.

Note that `update*` methods are called for both SCIM PUT and PATCH operations.

**Post-resource Modification**

They are called after the resource is persisted. The second parameter in these methods hold the object that was saved. Any change or manipulation upon the object will not be reflected in the underlying database, but may still modify the service response.

These methods are called if `getApiVersion` returns a number >= 2.

|Methods| 2nd param|2nd param Class/Link|
|:---|:---|:---|
|`postCreateUser`, `postUpdateUser`, `postDeleteUser`|`user`|[ScimCustomPerson](https://github.com/JanssenProject/jans/blob/1753d430037f623cfc11b99ceaa5d172676d9ecb/jans-scim/model/src/main/java/io/jans/scim/model/scim/ScimCustomPerson.java)|
|`postCreateGroup`, `postUpdateGroup`, `postDeleteGroup`|`group`|[GluuGroup](https://github.com/JanssenProject/jans/blob/6c1caa1c4c92d28571f8589cd701e6885d4d85ef/jans-scim/service/src/main/java/io/jans/scim/model/GluuGroup.java)|

Post-resource modification methods return a boolean. A `False` value aborts the corresponding SCIM operation and a 500 error is returned. The same applies if the method execution crashes at runtime.

Note that `postUpdate*` methods are called for both SCIM PUT and PATCH operations.

**Single Resource Retrieval**

These apply for SCIM operations that retrieve a resource by ID. They are called after the resource has been obtained from the database. The second parameter in these methods hold a reference to such object.

Any change or manipulation upon the object will not be reflected in the underlying database, but may still modify the service response.

These methods are called if `getApiVersion` returns a number >= 3.

|Methods|2nd param|2nd param Class/Link|
|:---|:---|:---|
|`getUser`|`user`|[ScimCustomPerson](https://github.com/JanssenProject/jans/blob/1753d430037f623cfc11b99ceaa5d172676d9ecb/jans-scim/model/src/main/java/io/jans/scim/model/scim/ScimCustomPerson.java)|
|`getGroup`|`group`|[GluuGroup](https://github.com/JanssenProject/jans/blob/6c1caa1c4c92d28571f8589cd701e6885d4d85ef/jans-scim/service/src/main/java/io/jans/scim/model/GluuGroup.java)|

Single resource retrieval methods return a boolean. A `False` value aborts the whole SCIM operation and a 500 error is returned. The same applies if the method execution crashes at runtime.

**Multiple Resources Retrieval**

These apply for SCIM search operations. They are called after the results have been obtained from the database. The second parameter in these methods hold a reference to such result set.

Any change or manipulation upon the object will not be reflected in the underlying database, but may still modify the service response.

These methods are called if `getApiVersion` returns a number >= 4.

|Methods|2nd param|2nd param Class/Link|
|:---|:---|:---|
|`postSearchUsers`|`results`|[PagedResult](https://github.com/JanssenProject/jans/blob/1753d430037f623cfc11b99ceaa5d172676d9ecb/jans-orm/core/src/main/java/io/jans/orm/model/PagedResult.java)|
|`postSearchGroups`|`results`|[PagedResult](https://github.com/JanssenProject/jans/blob/1753d430037f623cfc11b99ceaa5d172676d9ecb/jans-orm/core/src/main/java/io/jans/orm/model/PagedResult.java)|

Multiple resources retrieval methods return a boolean. A `False` value aborts the whole SCIM operation and a 500 error is returned. The same applies if the method execution crashes at runtime.

Note that searching using the root `.search` SCIM endpoint will trigger calls to both of the methods listed.

**Advanced control**

These are alternative methods that allow to tweak the response the service produces. They can be employed to introduce complex business rules when operations are executed.

These methods are called if `getApiVersion` returns a number >= 5.

|Methods|
|:---|
|`manageResourceOperation`
|`manageSearchOperation`

  
### Example: Modifying Search Results

SCIM spec defines the concept of attribute returnability where some attributes should never be part of a response (like passwords), always be returned (like resource identifiers), or be returned by default unless otherwise stated by the `excludedAttributes` parameter.

Assume you are maintaining a user base of secret agents that work for your company and need to avoid exposing information such as their physical addresses for safety reasons. To keep it simple let's restrict the scope to user searches only. In practice you should take steps to hide this data on user retrieval and update.

Let's alter `postSearchUsers`'s second parameter (`results`) to ensure addresses are not leaked:  
  
    for user in results.getEntries():
        user.setAttribute("jansAddres", None)
  
This is very straightforward code except for the usage of `jansAddres`. Shouldn't it be simply `addresses` as the known SCIM attribute?

Scripts work with entities that are about to be persisted or have already been saved so they kind of resemble the database structure (schema in LDAP terms). It turns out that database attribute names rarely match with SCIM names.

While it is easy to know the SCIM name of a database attribute, the converse requires checking the code, however since you already have the skill this shouldn't be a problem: in [this](https://github.com/JanssenProject/jans/blob/1753d430037f623cfc11b99ceaa5d172676d9ecb/jans-scim/model/src/main/java/io/jans/scim/model/scim2/user/UserResource.java) Java class you'll find the representation of a user resource in SCIM spec terms. Pay attention to the `addresses` field and its associated `StoreReference` annotation that contains the attribute where addresses are actually stored.

With that said, save your modifications. You may like the idea of adding some prints for enlightment like:
  
    print "%d entries returned of %d" % (results.getEntriesCount(), results.getTotalEntriesCount())
    for user in results.getEntries():
        print "Flushing addresses for user %s" % user.getUid() 
        user.setAttribute("jansAddres", None)
  
Ensure no addresses are returned anymore in your SCIM user searches. Happy testing!

### Controlling execution of SCIM operations

With the `manageResourceOperation` and `manageSearchOperation` methods you can make complex decisions on how processing should take place based on contextual data and the incoming payload.

**manageResourceOperation**
  
This method is invoked when any following operations are executed: resource creation, modification, removal and retrieval by ID. In case of bulks, the method is called for every operation that fits into these categories.

Parameters are described in the table below:
  
|Name|Description|Class/Link|
|:---|:---|:---
|`context`|Provides contextual information about the SCIM operation being called such as type of resource involved, HTTP verb, request headers, query params, etc.|[OperationContext](https://github.com/JanssenProject/jans/blob/6c1caa1c4c92d28571f8589cd701e6885d4d85ef/jans-scim/server/src/main/java/io/jans/scim/service/external/OperationContext.java)
|`entity`|A non-null object representing the resource involved|A descendant of [Entry](https://github.com/JanssenProject/jans/blob/1753d430037f623cfc11b99ceaa5d172676d9ecb/jans-orm/model/src/main/java/io/jans/orm/model/base/Entry.java). If the resource is a user, it will be an instance of [ScimCustomPerson](https://github.com/JanssenProject/jans/blob/1753d430037f623cfc11b99ceaa5d172676d9ecb/jans-scim/model/src/main/java/io/jans/scim/model/scim/ScimCustomPerson.java). In case of a group, it will be a [GluuGroup](https://github.com/JanssenProject/jans/blob/6c1caa1c4c92d28571f8589cd701e6885d4d85ef/jans-scim/service/src/main/java/io/jans/scim/model/GluuGroup.java)
|`payload`|The payload sent in the invocation; `null` when the operation is removal or retrieval by ID|The datatype depends on the operation called. Check the [interface](https://github.com/JanssenProject/jans/tree/6c1caa1c4c92d28571f8589cd701e6885d4d85ef/jans-scim/server/src/main/java/io/jans/scim/ws/rs/scim2) that suits best and inspect the first parameter's datatype. The class will belong to some subpackage inside [io.jans.scim.model.scim2](https://github.com/JanssenProject/jans/tree/1753d430037f623cfc11b99ceaa5d172676d9ecb/jans-scim/model/src/main/java/io/jans/scim/model/scim2)
  
This method is expected to return an instance of `javax.ws.rs.core.Response` that supersedes the output of the operation itself. In other words, the actual processing of the operation is skipped in favor of the code supplied here. However note that minor validations may take place in the payload before your code is actually called.

Returning `None` transfers the control of the operation for normal processing (default Jans implementation). If the method execution crashes at runtime, a 500 HTTP error is sent. 
  
**Notes:**
 
- Possible values for `context.getResourceType()` are: User, Group, FidoDevice, Fido2Device
- `context.getTokenDetails().getValue()` is a shortcut that will give you the access token the caller employed to issue the service call
- Both `context.getTokenDetails().getTokenType()` and `context.getTokenDetails().getScope()` return non null values when the protection mechanism of the API is OAuth or test mode
- Note that for resource creation operation, `entity` basically contains the same data supplied in the POST `payload`. In this case, `entity` has not originated from the database and has not been persisted either
- For the case of modification, retrieval and removal, `entity` contains the data currently stored in the database for the resource in question
- Since many values come from Java code, you can always do `getClass().getName()` to get an idea of what type of variables you are dealing with
- To build custom error responses your can reuse some of the `getErrorResponse` methods of class [BaseScimWebService](https://github.com/JanssenProject/jans/blob/6c1caa1c4c92d28571f8589cd701e6885d4d85ef/jans-scim/server/src/main/java/io/jans/scim/ws/rs/scim2/BaseScimWebService.java)
  
This method offers a high degree of flexibility. Perform careful testing of your code and account all potential scenarios.
  
**manageSearchOperation**
  
This method is invoked when resource searches are performed. Parameters are described in the table below:

|name|Description|Class/Link
|:---|:----|:---
|`context`|Provides contextual information about the SCIM operation being called such as type of resource involved, HTTP verb, request headers, query params, etc.|[OperationContext](https://github.com/JanssenProject/jans/blob/6c1caa1c4c92d28571f8589cd701e6885d4d85ef/jans-scim/server/src/main/java/io/jans/scim/service/external/OperationContext.java)
|`searchRequest`|An object representing the search parameters provided in the call (applies for both GET and POST)|[SearchRequest](https://github.com/JanssenProject/jans/blob/1753d430037f623cfc11b99ceaa5d172676d9ecb/jans-scim/model/src/main/java/io/jans/scim/model/scim2/SearchRequest.java)
  
Unlike `manageResourceOperation`, no `entity` parameter is passed. This is so because making decisions based on already executed searches would have a performance impact. Instead you can use `context.setFilterPrepend(...)` to help restrict the search against the database: here you can pass a String value that will be interpreted as an SCIM filter expression (see section 3.4.2.2 of RFC 7644). When the search being performed already contains a search filter (i.e. `searchRequest.getFilter()`is non-empty), a new filter is created by appending both "subfilters" with an `and` operator.

As in the case of `manageResourceOperation` this method is expected to return an instance of `javax.ws.rs.core.Response`.

Returning `None` transfers the control of the operation for normal processing (the Jans implementation). If the method execution crashes at runtime, a 500 HTTP error is sent.

The same recommendations given for `manageResourceOperation` apply here. If you build filter expressions in your method, ensure they are syntactically valid to avoid your callers getting unexpected "invalidFilter" 400 errors.

**Example: segmenting the user base**
  
Let's assume you make use of the SCIM attribute `userType` so that your user base is partitioned into three disjoint segments according to such attribute and you have designated its possible values to be: Contractor, Employee, or Intern.

Suppose your company has three different applications that make management of users in every category, that is, only one application is devoted to manage contractors, another employees, and other interns. You are interested in consistently granting access so that no application can create, query or modify users that don't belong to its focus.

To properly handle this multi-tenancy scenario, you decide to use the contextual information coming from every request to determine if the operation should be allowed or not. For this purpose you communicate every application developer to send an additional HTTP header in their call with a value that only you (the administrator) and the developer knows. Let's call it the "secret". For the sake of simplicity let's assume developers are external to the company and only you know their identities. They don't know each other so they cannot exchange secrets.

The strategy to implement segmentation is rather simple:

- Alter the default SCIM script by supplying a custom implementation for the methods that control execution

- Make the HTTP header name be a configuration property of the script so that it is not hard-coded

- Add a configuration property that contains the mapping of `userType` value vs. expected header value in JSON format

**Adding and parsing config properties**
  
Using Jans Config CLI update the SCIM script and add properties:

- `custom_header` with value `USER-SEGMENT-SECRET`
- `access_map` with value `{ "<random_string>":"Contractor", "<random_string>":"Employee", "<random_string>":"Intern" }`
  
Save the changes.

In the `init` method this properties should be parsed. To start, let's add some imports:
  
    from io.jans.scim.ws.rs.scim2 import BaseScimWebService
    import json
  
Here is how `init` would look like:
  
    def init(self, configurationAttributes):
        self.custom_header = configurationAttributes.get("custom_header").getValue2()
        access_map_json = configurationAttributes.get("access_map").getValue2()    
        self.access_map = json.loads(access_map_json)
        print "ScimEventHandler (init): Initialized successfully"
        return True
  
Note no validations took place here: we assumed the script contains the properties, that they are non empty and have sensible values.

**Allow/Deny resource operations**
  
The first step is to know the kind of application that is calling our service. For this purpose let's create a method that given incoming request headers returns the matching `userType`

    # headers params is an instance of javax.ws.rs.core.MultivaluedMap<String, String>
    def getUserType(self, headers):
        secret = headers.getFirst(self.custom_header)
        if secret in self.access_map:
            return self.access_map[secret]
        else:
            return None
  
Now let's code `manageResourceOperation`. We should allow access only under the following conditions:

- The `getUserType` method does not return `None`
- The entity object (`ScimCustomPerson` instance) has a proper `userType` value. This means that for user creation, the incoming payload comes with a matching `userType` and for the other cases, the already stored attribute matches as well
  
Assume that if the operation invoked is not user-related, we should allow access freely. Here is how the implementation might look:

    def manageResourceOperation(self, context, entity, payload, configurationAttributes):

        print "manageResourceOperation. SCIM endpoint invoked is %s (HTTP %s)" % (context.getPath(), context.getMethod()) 
        if context.getResourceType() != "User":
            return None

        expected_user_type = self.getUserType(context.getRequestHeaders())

        if expected_user_type != None and entity.getAttribute("jansUsrTyp") == expected_user_type:
            return None
        else:
            return BaseScimWebService.getErrorResponse(403, None, "Attempt to handle a not allowed user type")

<br/>Note no usage of the payload took place. A case you may like to evaluate is where mistakenly using an update operation, the `userType` is set to an unexpected value.

**Allow/Deny searches**
  
This time instead of inspecting an entity, we ought to make a filter expression to restrict the search when the database is queried. For your reference, a valid filter expression is for instance `userType eq "Contractor"`.

    def manageSearchOperation(self, context, searchRequest, configurationAttributes):

        print "manageSearchOperation. SCIM endpoint invoked is %s (HTTP %s)" % (context.getPath(), context.getMethod())

        resource_type = context.getResourceType()
        print "manageSearchOperation. This is a search over %s resources" % resource_type

        if resource_type != "User":
            return None

        expected_user_type = self.getUserType(context.getRequestHeaders())

        if expected_user_type != None:
            context.setFilterPrepend("userType eq \"%s\"" % expected_user_type)
            return None
        else:
            return BaseScimWebService.getErrorResponse(403, None, "Attempt to handle a not allowed user type")

The `manageSearchOperation` must return a `javax.ws.rs.core.Response`. A `None` value makes continue the operation processing normally.

## Working with more than one script
  
You may have already noticed that it is possible to have several scripts under the SCIM type. This is how execution takes place when there are several scripts enabled:

The applicable method is called in the first script. If the return value was `True`, the method is called again but this time in the subsequent script. If at any point a `False` return value is encountered, the SCIM operation is aborted with error 500. This means that a normal operation execution requires all involved methods across different scripts to be successful.

There is an important exception to the above and is related to the `manage*` methods. In this case, only one script takes effect (the first script found). Note that in most cases having a single SCIM script suffices for all needs. 

## Common Use Cases

## Script Type: Python

### [Custom SCIM Event Handler](sample-script/custom_scim_handler.py)

The sample code snippet shows how to -
1. Modify Search Results
2. Segment the user base
3. Allow/Deny resource operations
4. Allow/Deny searches

## Script Type: Java

### [Custom SCIM Event Handler](sample-script/custom_scim_handler.java)

The sample code snippet shows how to -
1. Modify Search Results
2. Segment the user base
3. Allow/Deny resource operations
4. Allow/Deny searches
