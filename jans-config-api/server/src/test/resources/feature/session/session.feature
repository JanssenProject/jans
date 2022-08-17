
Feature: Session flow

Background:
* def mainUrl = session_url

Scenario: Fetch all session
	Given url mainUrl 
	When method GET 
	Then status 401 
	And print response


Scenario: Fetch all session
	Given url mainUrl 
	And print 'accessToken = '+accessToken
	And header Authorization = 'Bearer ' + accessToken
	When method GET 
	Then status 200 
	And print response


@ignore		
Scenario: Revoke user session
	Given url mainUrl 
	And print 'accessToken = '+accessToken
	And header Authorization = 'Bearer ' + accessToken
	When method GET 
	Then status 200 
	And print response
	Then def result = response[0]
	And print result
	And def userDn = result.userDn  
	Given url mainUrl + '/' +userDn
	And header Authorization = 'Bearer ' + accessToken
	And request {}
	When method POST
	Then status 200
	And print response
	
