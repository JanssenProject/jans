
Feature: Openid connect sectors

    Background:
    * def mainUrl = openidsectors_url
    
Scenario: Fetch all openid connect sectors without bearer token
Given url mainUrl
When method GET
Then status 401


Scenario: Fetch all openid connect sectors
Given url mainUrl
And  header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response
And assert response.length != null
And assert response.length >= 0


@CreateUpdateDelete
Scenario: Create new OpenId Connect Sector
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And request read('sector.json')
When method POST
Then status 201
And print response
Then def result = response
Then set result.description = 'UpdatedQAAddedSector'
Then def inum_before = result.id
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And request result
When method PUT
Then status 200
And print response
And assert response.description == 'UpdatedQAAddedSector'
And assert response.id == inum_before
Given url mainUrl + '/' +response.id
And header Authorization = 'Bearer ' + accessToken
And header Content-Type = 'application/json-patch+json'
And header Accept = 'application/json'
And def newDescription = response.description
And print " newDescription = "+newDescription
And request "[ {\"op\":\"replace\", \"path\": \"/description\", \"value\":\""+newDescription+"\"} ]"
When method PATCH
Then status 200
And print response
And assert response.length !=0
Given url mainUrl + '/' +response.id
And header Authorization = 'Bearer ' + accessToken
When method DELETE
Then status 204
#And print response


Scenario: Delete a non-existion openid connect sector by inum
Given url mainUrl + '/1402.66633-8675-473e-a749'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404
And print response


Scenario: Get an openid connect sector by inum(unexisting sector
Given url mainUrl + '/53553532727272772'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404
And print response


Scenario: Get an openid connect sector by inum
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And request read('sector.json')
When method POST
Then status 201
And print response
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response
Given url mainUrl + '/' +response[0].id
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response
Given url mainUrl + '/' +response.id
And header Authorization = 'Bearer ' + accessToken
When method DELETE
Then status 204
And print response



