Feature: Attributes

@CreateUpdateDelete
Scenario: Create new attribute
Given url attributes_url
And header Authorization = 'Bearer ' + accessToken
And request read('classpath:attribute.json')
When method POST
Then status 201
Then def result = response
Then set result.displayName = 'UpdatedQAAddedAttribute'
Then def inum_before = result.inum
Given url attributes_url
And header Authorization = 'Bearer ' + accessToken
And request result
When method PUT
Then status 200
And assert response.displayName == 'UpdatedQAAddedAttribute'
And assert response.inum == inum_before
Given url attributes_url + '/' +response.inum
And header Authorization = 'Bearer ' + accessToken
When method DELETE
Then status 204

Scenario: Delete a non-existion attribute by inum
Given url attributes_url + '/1402.66633-8675-473e-a749'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404


Scenario: Get an attribute by inum(unexisting attribute)
Given url attributes_url + '/53553532727272772'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404

Scenario: Get an attribute by inum
Given url attributes_url
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
Given url attributes_url + '/' +response[0].inum
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200