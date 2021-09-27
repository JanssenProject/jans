
Feature: Scim Users 

Background:
* def mainUrl = scim_url

Scenario: Fetch scim users without bearer token 
	Given url mainUrl 
	When method GET 
	Then status 401 


Scenario: Search user by filter 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And param pattern = 'city' 
  	When method GET 
	Then status 200
	And print response 
	#And assert response.length != 0 


Scenario: Get an scim user by id 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 200 
	Given url mainUrl + '/' +response[0].id
	And header Authorization = 'Bearer ' + accessToken
	When method GET 
	Then status 200
	And print response


@ignore
@CreateUpdateDelete 
Scenario: Create new user, update and delete 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And request read('scim-user.json') 
	When method POST 
	Then status 201 
	Then def result = response 
	Then set result.displayName = 'UpdatedQAAddedAttribute' 
	Then def inum_before = result.inum 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And request result 
	When method PUT 
	Then status 200 
	And assert response.displayName == 'UpdatedQAAddedAttribute' 
	And assert response.inum == inum_before 
	Given url mainUrl + '/' +response.inum
	And header Authorization = 'Bearer ' + accessToken 
	When method DELETE 
	Then status 204 


@ignore
Scenario: Patch scim user
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And param pattern = 'street'
    And param limit = 1 
	When method GET 
	Then status 200
	And print response 
	And assert response.length == 1
	Then def result = response[0] 
	And def id_before = result.id  
	And print 'id = '+id_before
    And print result.nickName
    And def orig_nickName = (result.nickName == null ? "New_nickName" : result.nickName)
    And print 'orig_nickName = '+orig_nickName
    And def request_body = (result.nickName == null ? "[ {\"op\":\"add\", \"path\": \"/nickName\", \"value\":"+orig_nickName+" } ]" : "[ {\"op\":\"replace\", \"path\": \"/nickName\", \"value\":"+orig_nickName+" } ]")
    And print 'request_body ='+request_body
  	Given url  mainUrl + '/' +id_before
  	And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request request_body
	Then print request
    When method PATCH
    Then status 200
    And print response
  
	