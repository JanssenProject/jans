---
name: User Story
about: Describe a feature from the perspective of the user or customer who will use
  it.
title: ''
labels: User Story
assignees: ''

---

### User Story Title.

The title should briefly describe what the end-user wishes to achieve or the business/technical value should get from this feature. For example:

```
API Consumer creates a new OpenID client.
```

### User Story Meta Information.

#### Risk Level. 

This allows us to assign a risk level to the user story, which represents the level of uncertainty around its completion. Also graded from 1 (Low risk) to 5 (Very High Risk)

#### Effort.

 This allows us to assign how much work needs to be done to get a user story complete. This will be an integer to be designated by the development team.

#### Priority.

 This allows us to assign a priority to the user story as HIGH, MEDIUM and LOW.

### Description.
 This is the core of the user story. It should be relatively short (3 to 5 lines) and should describe the high-level requirement or requirements of the feature. For example:

```
As an API Consumer, I want to make a request to the oxauth-config-api server containing the details of an OpenID client so that I can create a new OpenID client.
```

#### Acceptance Criteria. This section contains every condition under which the feature should operate for the end-user to consider the feature to be accepted or marked complete. The criteria also serve as a guideline for writing unit tests for the feature. For example:

```
When the API Consumer sends a request to POST /api/v1/openid/clients containing valid OpenID Client creation data
Then the oxauth-config-api server will create a new OpenID client
And will return a HTTP 201 Created Response
Also when the the API Consumer sees the 201 Response
And a JSON response body containing the details of the newly created client
Request: {}
Response: {}
```

 As you can see , requests and responses should be included in the ticket. If they are too large, create a folder for sample requests and responses and provide links to them in the Notes section below.

### Notes. 
Additional important information can be placed here. Links, references, etc.
