
Feature: Openid connect Scopes

Background:
* def mainUrl = scopes_url

Scenario: Fetch all openid connect scopes without bearer token
Given url mainUrl
When method GET
Then status 401


Scenario: Fetch all scopes
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response
And assert response.length != null


Scenario: Fetch all openid connect scopes
Given url mainUrl
And  header Authorization = 'Bearer ' + accessToken
And param type = 'openid'
When method GET
Then status 200
And print response
And assert response.length != null


Scenario: Fetch the first three openidconnect scopes
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And param type = 'openid'
And param limit = 3
When method GET
Then status 200
And print response
#And assert response.length == 3


Scenario: Search openid connect scopes given a serach pattern
Given url mainUrl
And  header Authorization = 'Bearer ' + accessToken
And param type = 'openid'
And param pattern = 'openid'
When method GET
Then status 200
And print response
#And assert response.length == 1

Scenario: Fetch scopes based on creator
Given url mainUrl + '/creator/abc'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response

Scenario: Fetch scopes based on type
Given url mainUrl + '/type/uma'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response


@CreateUpdateDelete
Scenario: Create new OpenId Connect Scope
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And request read('scope.json')
When method POST
Then status 201
And print response
Then def result = response
Then set result.displayName = 'UpdatedQAAddedScope'
Then def inum_before = result.inum
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And request result
When method PUT
Then status 200
And print response
And assert response.displayName == 'UpdatedQAAddedScope'
And assert response.inum == inum_before
Given url mainUrl + '/' +response.inum
And header Authorization = 'Bearer ' + accessToken
And header Content-Type = 'application/json-patch+json'
And header Accept = 'application/json'
And def newDisplayName = response.displayName
And print " newDisplayName = "+newDisplayName
And request "[ {\"op\":\"replace\", \"path\": \"/displayName\", \"value\":\""+newDisplayName+"\"} ]"
When method PATCH
Then status 200
And print response
And assert response.length !=0
Given url mainUrl + '/' +response.inum
And header Authorization = 'Bearer ' + accessToken
When method DELETE
Then status 204


Scenario: Delete a non-existing openid connect scope by inum
Given url mainUrl + '/1402.66633-8675-473e-a749'
And header Authorization = 'Bearer ' + accessToken
And param type = 'openid'
When method GET
Then status 404
And print response


Scenario: Get an openid connect scope by inum(unexisting scope)
Given url mainUrl + '/53553532727272772'
And header Authorization = 'Bearer ' + accessToken
And param type = 'openid'
When method GET
Then status 404
And print response


Scenario: Get an openid connect scopes by inum
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response
Given url mainUrl + '/' +response.entries[0].inum
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response
