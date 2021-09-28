
Feature: Scim Users 

Background:
* def mainUrl = scim_url


Scenario: Fetch scim users without bearer token 
	Given url mainUrl 
	When method GET 
	Then status 401 

    
@Get-all
Scenario: Search user by filter 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
    And param filter = 'userName co \"mi\"' 
  	When method GET 
	Then status 200
	And print response 
	#And assert response.length != 0 

    
@GetById
Scenario: Get an scim user by id 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 200 
    And print response
    And print response.Resources[0].id
	Given url mainUrl + '/' +response.Resources[0].id
	And header Authorization = 'Bearer ' + accessToken
	When method GET 
	Then status 200
	And print response
 
 
@SearchRequest 
Scenario: Search user using SearchRequest
	Given url mainUrl + '/.search'
	And header Authorization = 'Bearer ' + accessToken 
	And request read('scim-search-request.json') 
	When method POST 
	Then status 200
    And print response  



@CreateUpdateDelete 
Scenario: Create new user, update and delete 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And request read('scim-user.json') 
	When method POST 
	Then status 201
    And print response    
	Then def result = response 
    Then def inum = result.id 
    Then def updated_displayName = 'Updated ' +result.displayName
	Then set result.displayName = updated_displayName	
    And print 'Updating displayName'
	Given url mainUrl + '/' +inum
	And header Authorization = 'Bearer ' + accessToken 
	And request result 
	When method PUT 
	Then status 200 
    And print response
	And assert response.displayName == updated_displayName
	And assert response.id == inum
    And print 'Successfully updated displayName'
	Given url mainUrl + '/' +inum
	And header Authorization = 'Bearer ' + accessToken 
	When method DELETE 
	Then status 204 
     And print 'User successfully deleted'


@PatchUser
Scenario: Patch scim user
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 200 
    And print response
    And print response.Resources[0].id
	Then def result = response.Resources[0] 
    Then def inum = result.id 
    And print result.displayName
    And def patched_displayName = (result.displayName == null ? 'Patched displayName' : result.displayName)
    And def request_body = (result.displayName == null ? "[ {\"op\":\"add\", \"path\": \"/displayName\", \"value\":"+patched_displayName+" } ]" : "[ {\"op\":\"replace\", \"path\": \"/displayName\", \"value\":"+patched_displayName+" } ]")
    And print 'Patching displayName' 
	Given url mainUrl + '/' +inum
	And header Authorization = 'Bearer ' + accessToken 
    And header Accept = 'application/json'
    And request request_body
	Then print request
    When method PATCH
    Then status 200
    And print response
  
	