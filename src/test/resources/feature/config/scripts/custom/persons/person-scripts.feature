Feature: Person Custom Scripts

Background:
  	* def personscripts_url = scriptsUrl
@ignore  	
Scenario: Fetch all person custom scripts without bearer token
Given url personscripts_url
And path 'person_authentication'
And  header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response
And assert response.length != null

@ignore
Scenario: Fetch all person custom scripts
Given url personscripts_url
And  header Authorization = 'Bearer ' + accessToken
And params { type:'PERSON_AUTHENTICATION'}
When method GET
Then status 200
And print response
And assert response.length != null
And assert response[0].scriptType == 'PERSON_AUTHENTICATION'

@ignore
Scenario: Fetch the first three person custom scripts
Given url personscripts_url
And  header Authorization = 'Bearer ' + accessToken
#And param type =  'PERSON_AUTHENTICATION'
#And param limit = 3
And params ({ type: PERSON_AUTHENTICATION, limit: 3 })
When method GET
And print response
Then status 200
And assert response.length == 3
And assert response[0].scriptType == 'PERSON_AUTHENTICATION'

@ignore
Scenario: Search person custom scripts given a serach pattern
Given url personscripts_url
And  header Authorization = 'Bearer ' + accessToken
And param type =  'PERSON_AUTHENTICATION'
And param pattern = 'fido2'
When method GET
And print response
Then status 200
And assert response.length == 1
And assert response[0].scriptType == 'PERSON_AUTHENTICATION'

@ignore
@CreateUpdateDelete
Scenario: Create new Person Script
Given url personscripts_url
And header Authorization = 'Bearer ' + accessToken
And request read('classpath:person-script.json')
When method POST
And print response
Then status 201
Then def result = response
Then set result.name = 'UpdatedQAAddedPersonScript'
Then def inum_before = result.inum
Given url personscripts_url
And header Authorization = 'Bearer ' + accessToken
And request result
When method PUT
And print response
Then status 200
And assert response.name == 'UpdatedQAAddedPersonScript'
And assert response.inum == inum_before
Given url personscripts_url + '/' +response.inum
And header Authorization = 'Bearer ' + accessToken
And print response
When method DELETE
Then status 204

@ignore
Scenario: Delete a non-existing person custom script by inum
Given url personscripts_url + '/1402.66633-8675-473e-a749'
And header Authorization = 'Bearer ' + accessToken
When method GET
And print response
Then status 404

@ignore
Scenario: Get a person custom script by inum(unexisting person script)
Given url personscripts_url + '/53553532727272772'
And header Authorization = 'Bearer ' + accessToken
When method GET
And print response
Then status 404

@ignore
Scenario: Get a person custom script by inum
Given url personscripts_url
And header Authorization = 'Bearer ' + accessToken
And param type =  'PERSON_AUTHENTICATION'
When method GET
And print response
Then status 200
Given url personscripts_url + '/' +response[0].inum
And header Authorization = 'Bearer ' + accessToken
And param type =  'PERSON_AUTHENTICATION'
When method GET
And print response
Then status 200
And assert response.scriptType == 'PERSON_AUTHENTICATION'