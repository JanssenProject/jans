Feature: Uma Resource

Scenario: Fetch all uma resources without bearer token
Given url umaresources_url
When method GET
Then status 401

Scenario: Fetch all uma resources
Given url umaresources_url
And  header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response
And assert response.length != null

Scenario: Fetch the first two uma resources
Given url umaresources_url
And  header Authorization = 'Bearer ' + accessToken
And param limit = 2
When method GET
Then status 200
And assert response.length == 2

Scenario: Search uma resources given a search pattern
Given url umaresources_url
And  header Authorization = 'Bearer ' + accessToken
And param pattern = 'Passport Resource'
When method GET
Then status 200
#And assert response.length == 1


@CreateUpdateDelete
Scenario: Create new Uma Resource
Given url umaresources_url
And header Authorization = 'Bearer ' + accessToken
And request read('classpath:uma-resource.json')
When method POST
Then status 201
Then def result = response
Then set result.name = 'UpdatedQAAddedResource'
Then def id_before = result.id
Given url umaresources_url
And header Authorization = 'Bearer ' + accessToken
And request result
When method PUT
Then status 200
And assert response.name == 'UpdatedQAAddedResource'
And assert response.id == id_before
Given url umaresources_url + '/' +response.id
And header Authorization = 'Bearer ' + accessToken
When method DELETE
Then status 204

Scenario: Delete a non-existion uma resource by id
Given url umaresources_url + '/1402.66633-8675-473e-a749'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404

Scenario: Get an uma resource by id(unexisting resource)
Given url umaresources_url + '/53553532727272772'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404

Scenario: Get an uma resource by id
Given url umaresources_url
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
Given url umaresources_url + '/' +response[0].id
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200