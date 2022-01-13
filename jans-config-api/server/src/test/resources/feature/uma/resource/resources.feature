
Feature: Uma Resource

    Background:
    * def mainUrl = umaresources_url

Scenario: Fetch all uma resources without bearer token
Given url mainUrl
When method GET
Then status 401


Scenario: Fetch all uma resources
Given url mainUrl
And print accessToken
And  header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response
#And assert response.length != null


Scenario: Fetch the first two uma resources
Given url mainUrl
And print accessToken
And  header Authorization = 'Bearer ' + accessToken
And param limit = 2
When method GET
Then status 200
Then print response
#And assert response.length == 2


Scenario: Search uma resources given a search pattern
Given url mainUrl
And  header Authorization = 'Bearer ' + accessToken
And param pattern = 'Passport Resource'
When method GET
Then status 200
#And assert response.length == 1


@CreateUpdateDelete
Scenario: Create new Uma Resource
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And request read('uma-resource.json')
When method POST
Then status 201
And print response
Then def result = response
Then set result.name = 'UpdatedQAAddedResource'
Then def id_before = result.id
Given url mainUrl + '/' +response.id
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And request result
When method PUT
Then status 200
And print response
And assert response.name == 'UpdatedQAAddedResource'
And assert response.id == id_before
Given url mainUrl + '/' +response.id
And header Authorization = 'Bearer ' + accessToken
And header Content-Type = 'application/json-patch+json'
And header Accept = 'application/json'
And def newName = response.name
And print " newName = "+newName
And request "[ {\"op\":\"replace\", \"path\": \"/description\", \"value\":\""+newName+"\"} ]"
When method PATCH
Then status 200
And print response
And assert response.length !=0
Given url mainUrl + '/' +response.id
And header Authorization = 'Bearer ' + accessToken
When method DELETE
Then status 204


Scenario: Delete a non-existion uma resource by id
Given url mainUrl + '/1402.66633-8675-473e-a749'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404


Scenario: Get an uma resource by id(unexisting resource)
Given url mainUrl + '/53553532727272772'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404


@ignore
Scenario: Get an uma resource by id
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
Given url mainUrl + '/' +response[0].id
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
