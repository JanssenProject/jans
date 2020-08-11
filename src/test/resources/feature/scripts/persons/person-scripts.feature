Feature: Person Custom Scripts

Scenario: Fetch all person custom scripts without bearer token
Given url personscripts_url
When method GET
Then status 401

Scenario: Fetch all person custom scripts
Given url personscripts_url
And  header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And assert response.length != null
And assert response[0].scriptType == 'PERSON_AUTHENTICATION'

Scenario: Fetch the first three person custom scripts
Given url personscripts_url
And  header Authorization = 'Bearer ' + accessToken
And param limit = 3
When method GET
Then status 200
And assert response.length == 3
And assert response[0].scriptType == 'PERSON_AUTHENTICATION'

Scenario: Search person custom scripts given a serach pattern
Given url personscripts_url
And  header Authorization = 'Bearer ' + accessToken
And param pattern = 'fido2'
When method GET
Then status 200
And assert response.length == 1
And assert response[0].scriptType == 'PERSON_AUTHENTICATION'

@CreateUpdateDelete
Scenario: Create new Person Script
Given url personscripts_url
And header Authorization = 'Bearer ' + accessToken
And request read('classpath:person-script.json')
When method POST
Then status 201
Then def result = response
Then set result.name = 'UpdatedQAAddedPersonScript'
Then def inum_before = result.inum
Given url personscripts_url
And header Authorization = 'Bearer ' + accessToken
And request result
When method PUT
Then status 200
And assert response.name == 'UpdatedQAAddedPersonScript'
And assert response.inum == inum_before
Given url personscripts_url + '/' +response.inum
And header Authorization = 'Bearer ' + accessToken
When method DELETE
Then status 204

Scenario: Delete a non-existing person custom script by inum
Given url personscripts_url + '/1402.66633-8675-473e-a749'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404


Scenario: Get a person custom script by inum(unexisting person script)
Given url personscripts_url + '/53553532727272772'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404

Scenario: Get a person custom script by inum
Given url personscripts_url
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
Given url personscripts_url + '/' +response[0].inum
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And assert response.scriptType == 'PERSON_AUTHENTICATION'