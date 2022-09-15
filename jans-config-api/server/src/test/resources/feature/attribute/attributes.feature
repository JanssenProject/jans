
Feature: Attributes 

Background:
* def mainUrl = attributes_url

Scenario: Fetch all attributes without bearer token 
	Given url mainUrl 
	When method GET 
	Then status 401 


Scenario: Fetch all attributes 
	Given url mainUrl 
	And print 'accessToken = '+accessToken
	And print 'issuer = '+issuer
	And header Authorization = 'Bearer ' + accessToken
	#And header issuer = issuer  
	When method GET 
	Then status 200 
	And print response
	#And assert response.length != null 
	#And assert response.length >= 10 

@ignore
Scenario: Fetch based on filter
	Given url mainUrl 
	And print 'accessToken = '+accessToken
	And print 'issuer = '+issuer
	And header Authorization = 'Bearer ' + accessToken
	#And header issuer = issuer 
	And param limit = 3 	
	And param pattern = 'edu' 
	And param startIndex = 1 
	When method GET 
	Then status 200 
	And print response
	#And assert response.length != null 
	#And assert response.length >= 10 

Scenario: Fetch the first three attributes 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken
	And param limit = 3 
	When method GET 
	Then status 200
	And print response 
	#And assert response.length == 3 


Scenario: Search attributes given a search pattern 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And param pattern = 'city' 
  	When method GET 
	Then status 200
	And print response 
	#And assert response.length != 0 

Scenario: Fetch all active attributes 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And param status = 'active' 
	When method GET 
	Then status 200
	And print response 

Scenario: Fetch the first three active attributes 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And param limit = 3 
	And param status = 'active' 
	When method GET 
	Then status 200
	And print response 
	#And assert response.length == 3 
	#And assert response[0].status == 'ACTIVE'
	#And assert response[1].status == 'ACTIVE'
	#And assert response[2].status == 'ACTIVE'	


Scenario: Fetch the first three inactive attributes 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And param limit = 3 
	And param status = 'inactive' 
	When method GET 
	Then status 200
	And print response 
	#And assert response.length == 3 
	#And assert response[0].status == 'INACTIVE'
	#And assert response[1].status == 'INACTIVE'
	#And assert response[2].status == 'INACTIVE'		


@CreateUpdateDelete 
Scenario: Create new attribute 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And request read('attribute.json') 
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


Scenario: Delete a non-existion attribute by inum 
	Given url mainUrl + '/1402.66633-8675-473e-a749'
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 404 
	

Scenario: Get an attribute by inum(unexisting attribute) 
	Given url mainUrl + '/53553532727272772'
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 404 

@ignore
Scenario: Get an attribute by inum 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 200 
	Given url mainUrl + '/' +response[0].inum
	And header Authorization = 'Bearer ' + accessToken
	When method GET 
	Then status 200
	And print response


@CreateUpdate 
Scenario: Create new attribute 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And request read('attribute-for-patch.json') 
	When method POST 
	Then status 201 
	Then def result = response 
	Then set result.jansHideOnDiscovery = 'true' 
	Then def inum_before = result.inum 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And request result 
	When method PUT 
	Then status 200 
	And print response
	And print response.inum
	Given url mainUrl + '/' +response.inum
	And header Authorization = 'Bearer ' + accessToken 
	When method DELETE 
	Then status 204 
	
@ignore
Scenario: Patch jansHideOnDiscovery configuration for Country attribute
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And param pattern = 'street'
    And param limit = 1 
	When method GET 
	Then status 200
	And print response 
	And assert response.length == 1
	Then def result = response[0] 
	And def inum_before = result.inum  
	And print 'inum = '+inum_before
    And print result.jansHideOnDiscovery
    And def orig_jansHideOnDiscovery = (result.jansHideOnDiscovery == null ? false : result.jansHideOnDiscovery)
    And print 'orig_jansHideOnDiscovery = '+orig_jansHideOnDiscovery
    #And def new_jansHideOnDiscovery = (orig_jansHideOnDiscovery == null || orig_jansHideOnDiscovery == false ? true : false) 
    And def request_body = (result.jansHideOnDiscovery == null ? "[ {\"op\":\"add\", \"path\": \"/jansHideOnDiscovery\", \"value\":"+orig_jansHideOnDiscovery+" } ]" : "[ {\"op\":\"replace\", \"path\": \"/jansHideOnDiscovery\", \"value\":"+orig_jansHideOnDiscovery+" } ]")
    And print 'request_body ='+request_body
  	Given url  mainUrl + '/' +inum_before
  	And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request request_body
	Then print request
    When method PATCH
    Then status 200
    And print response
  
	