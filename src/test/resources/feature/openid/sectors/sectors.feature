@ignore
Feature: Openid connect sectors

Scenario: Fetch all openid connect sectors without bearer token
Given url openidsectors_url
When method GET
Then status 401

Scenario: Fetch all openid connect sectors
Given url openidsectors_url
And  header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response
And assert response.length != null
And assert response.length >= 0

@CreateUpdateDelete
Scenario: Create new OpenId Connect Sector
Given url openidsectors_url
And header Authorization = 'Bearer ' + accessToken
And request read('sector.json')
When method POST
Then status 201
And print response
Then def result = response
Then set result.description = 'UpdatedQAAddedSector'
Then def inum_before = result.id
Given url openidsectors_url
And header Authorization = 'Bearer ' + accessToken
And request result
When method PUT
Then status 200
And print response
And assert response.description == 'UpdatedQAAddedSector'
And assert response.id == inum_before
Given url openidsectors_url + '/' +response.id
And header Authorization = 'Bearer ' + accessToken
When method DELETE
Then status 204
#And print response

Scenario: Delete a non-existion openid connect sector by inum
Given url openidsectors_url + '/1402.66633-8675-473e-a749'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404
And print response

Scenario: Get an openid connect sector by inum(unexisting sector
Given url openidsectors_url + '/53553532727272772'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404
And print response

Scenario: Get an openid connect sector by inum
Given url openidsectors_url
And header Authorization = 'Bearer ' + accessToken
And request read('sector.json')
When method POST
Then status 201
And print response
Given url openidsectors_url
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response
Given url openidsectors_url + '/' +response[0].id
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response
Given url openidsectors_url + '/' +response.id
And header Authorization = 'Bearer ' + accessToken
When method DELETE
Then status 204
And print response