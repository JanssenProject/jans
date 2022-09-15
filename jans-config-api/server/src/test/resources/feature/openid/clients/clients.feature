
Feature: Openid connect clients

    Background:
    * def mainUrl = openidclients_url

Scenario: Fetch all openid connect clients without bearer token
Given url mainUrl
When method GET
Then status 401


Scenario: Fetch all openid connect clients
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response
#And assert response.length != null


Scenario: Fetch the first three openidconnect clients
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And param limit = 3
When method GET
Then status 200
And print response
#And assert response.length == 3


Scenario: Search openid connect clients given a serach pattern
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And param limit = 1
When method GET
Then status 200
And print response
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And param pattern = response.entries[0].displayName
And print 'pattern = '+pattern 
When method GET
Then status 200
And print response
#And assert response.length !=0

Scenario: Search openid connect clients given a serach pattern and pagination
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And param pattern = 'test'
And param limit = 10
And param startIndex = 1
When method GET
Then status 200
And print response

Scenario: Get an openid connect client by inum(unexisting client)
Given url mainUrl + '/53553532727272772'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404


Scenario: Get an openid connect client by inum
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
Given url mainUrl + '/' +response.entries[0].inum
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response

@ignore
@CreateUpdateDelete
Scenario: Create new OpenId Connect Client
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And request read('client.json')
When method POST
Then status 201
And print response
Then def result = response
Then set result.entries[0].displayName = 'UpdatedQAAddedClient'
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And request result
When method PUT
Then status 200
And print response
And assert response.entries[0]displayName == 'UpdatedQAAddedClient'
Given url mainUrl + '/' +response.entries[0].inum
And header Authorization = 'Bearer ' + accessToken
When method DELETE
Then status 204
And print response


Scenario: Delete a non-existion openid connect client by inum
Given url mainUrl + '/1402.66633-8675-473e-a749'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404
And print response

@ignore
Scenario: Patch openid connect client
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And param limit = 1
When method GET
Then status 200
And print response
Given url mainUrl + '/' +response.entries[0].inum
And header Authorization = 'Bearer ' + accessToken
And header Content-Type = 'application/json-patch+json'
And header Accept = 'application/json'
And def newName = response.entries[0].displayName
And print " newName = "+newName
#And request "[ {\"op\":\"replace\", \"path\": \"/displayName\", \"value\":\""+newName+"\"} ]"
And def request_body = (response.entries[0].displayName == null ? "[ {\"op\":\"add\", \"path\": \"/displayName\", \"value\":null } ]" : "[ {\"op\":\"replace\", \"path\": \"/displayName\", \"value\":"+response.displayName+" } ]")
And print 'request_body ='+request_body
And request request_body
When method PATCH
Then status 200
And print response
#And assert response.length !=0

@ignore
@CreateUpdateDelete
Scenario: Create new OpenId Connect Client
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And request read('openid_clients_create.json')
When method POST
Then status 201
And print response


