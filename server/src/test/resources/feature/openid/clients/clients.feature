
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
And assert response.length != null


Scenario: Fetch the first three openidconnect clients
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And param limit = 3
When method GET
Then status 200
And print response
And assert response.length == 3


Scenario: Search openid connect clients given a serach pattern
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And param limit = 1
When method GET
Then status 200
And print response
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And param pattern = response[0].displayName
And print 'pattern = '+pattern 
When method GET
Then status 200
And print response
And assert response.length !=0


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
Given url mainUrl + '/' +response[0].inum
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response


@CreateUpdateDelete
Scenario: Create new OpenId Connect Client
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And request read('client.json')
When method POST
Then status 201
Then def result = response
Then set result.displayName = 'UpdatedQAAddedClient'
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And request result
When method PUT
Then status 200
And assert response.displayName == 'UpdatedQAAddedClient'
Given url mainUrl + '/' +response.inum
And header Authorization = 'Bearer ' + accessToken
When method DELETE
Then status 204


Scenario: Delete a non-existion openid connect client by inum
Given url mainUrl + '/1402.66633-8675-473e-a749'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404


Scenario: Patch openid connect client
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And param limit = 1
When method GET
Then status 200
And print response
Given url mainUrl + '/' +response[0].inum
And header Authorization = 'Bearer ' + accessToken
And header Content-Type = 'application/json-patch+json'
And header Accept = 'application/json'
And def newName = response[0].displayName
And print " newName = "+newName
#And request "[ {\"op\":\"replace\", \"path\": \"/displayName\", \"value\":\""+newName+"\"} ]"
And def request_body = (response.displayName == null ? "[ {\"op\":\"add\", \"path\": \"/displayName\", \"value\":null } ]" : "[ {\"op\":\"replace\", \"path\": \"/displayName\", \"value\":"+response.displayName+" } ]")
And print 'request_body ='+request_body
And request request_body
When method PATCH
Then status 200
And print response
And assert response.length !=0

