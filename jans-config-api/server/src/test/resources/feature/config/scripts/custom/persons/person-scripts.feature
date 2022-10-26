
Feature: Person Custom Scripts

Background:
  * def mainUrl = scriptsUrl
	
Scenario: Fetch all person custom scripts without bearer token
Given url mainUrl + '/type'
And path 'person_authentication'
When method GET
Then status 401
And print response


Scenario: Fetch all person custom scripts
Given url mainUrl + '/type'
And header Authorization = 'Bearer ' + accessToken
And path 'person_authentication'
When method GET
Then status 200
And print response
And assert response.length != null
And assert response.entries[0].scriptType == 'person_authentication'


Scenario: Fetch the first three person custom scripts
Given url mainUrl + '/type'
And header Authorization = 'Bearer ' + accessToken
And path 'person_authentication'
And params ({ limit: 3})
When method GET
And print response
Then status 200
And assert response.entries.length == 3
And assert response.entries[0].scriptType == 'person_authentication'


Scenario: Search person custom scripts given a serach pattern
Given url mainUrl + '/type'
And header Authorization = 'Bearer ' + accessToken
And path 'person_authentication'
And params ({ limit: 3,pattern:'fido2'})
When method GET
And print response
Then status 200
And assert response.entries.length <= 3
And assert response.entries[0].scriptType == 'person_authentication'


@CreateUpdateDelete
Scenario: Create new Person Script
Given url mainUrl + '/type'
And header Authorization = 'Bearer ' + accessToken
And path 'person_authentication'
When method GET
And print response
Then status 200
And assert response.length != 0
And assert response.entries[0].scriptType == 'person_authentication'
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And def testScript = response.entries[0]
And print "testScript before = "+testScript
And testScript.inum = null
And testScript.dn = null
And testScript.name = "Test_PERSON_AUTHENTICATION"
And testScript.description = "Test_PERSON_AUTHENTICATION_description"
And print "testScript after = "+testScript
And request testScript
When method POST
And print response
Then status 201
Then def result = response
Then set result.name = 'UpdatedQAAddedPersonScript'
Then def inum_before = result.inum
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And request result
When method PUT
And print response
Then status 200
And assert response.name == 'UpdatedQAAddedPersonScript'
And assert response.inum == inum_before
Given url mainUrl + '/' +response.inum
And header Authorization = 'Bearer ' + accessToken
And print response
When method DELETE
Then status 204


Scenario: Delete a non-existing person custom script by inum
Given url mainUrl + '/1402.66633-8675-473e-a749'
And header Authorization = 'Bearer ' + accessToken
When method DELETE
And print response
Then status 404


Scenario: Get a person custom script by inum(unexisting person script)
#Given url mainUrl + '/53553532727272772'
Given url mainUrl + '/inum/53553532727272772'
And header Authorization = 'Bearer ' + accessToken
When method GET
And print response
Then status 404


Scenario: Get a person custom script by inum
Given url mainUrl + '/type'
And header Authorization = 'Bearer ' + accessToken
And path 'person_authentication'
When method GET
And print response
Then status 200
And print response.entries[0].inum
Given url mainUrl + '/inum/'+response.entries[0].inum
And header Authorization = 'Bearer ' + accessToken
And print request
When method GET
And print response
Then status 200
And assert response.scriptType == 'person_authentication'