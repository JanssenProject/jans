
Feature: Uma Scopes

Background:
* def mainUrl = scopes_url


Scenario: Fetch all uma scopes without bearer token
Given url mainUrl
When method GET
Then status 401


Scenario: Fetch all uma scopes
Given url mainUrl
And print accessToken
And  header Authorization = 'Bearer ' + accessToken
And param type = 'uma'
When method GET
Then status 200
And print response
And assert response.length != null


Scenario: Fetch the first three uma scopes
Given url mainUrl
And  header Authorization = 'Bearer ' + accessToken
And param type = 'uma'
And param limit = 3
When method GET
Then status 200
And print response
#And assert response.length <= 3


Scenario: Search uma scopes given a search pattern
Given url mainUrl
And  header Authorization = 'Bearer ' + accessToken
And param type = 'uma'
And param pattern = 'SCIM Access'
When method GET
Then status 200
And print response
#And assert response.length == 1


@CreateUpdateDelete
Scenario: Create new Uma Scope
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And request read('uma-scope.json')
When method POST
Then status 201
And print response
Then def result = response
Then set result.displayName = 'UpdatedQAAddedUmaScope'
Then def inum_before = result.inum
Given url mainUrl + '/' +result.inum
And header Authorization = 'Bearer ' + accessToken
And param type = 'uma'
When method GET
Then status 200
And print response
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And request result
When method PUT
Then status 200
And print response
And assert response.displayName == 'UpdatedQAAddedUmaScope'
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


Scenario: Delete a non-existion uma scope by inum
Given url mainUrl + '/1402.66633-8675-473e-a749'
And header Authorization = 'Bearer ' + accessToken
And param type = 'uma'
When method GET
Then status 404


Scenario: Get an uma scope by inum(unexisting scope)
Given url mainUrl + '/53553532727272772'
And header Authorization = 'Bearer ' + accessToken
And param type = 'uma'
When method GET
Then status 404
And print response


@ignore
Scenario: Get an uma scope by inum
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And param type = 'uma'
When method GET
Then status 200
Given url mainUrl + '/' +response[0].inum
And header Authorization = 'Bearer ' + accessToken
And param type = 'uma'
When method GET
Then status 200

