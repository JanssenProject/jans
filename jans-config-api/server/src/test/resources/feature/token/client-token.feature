
Feature: Token flow

Background:
* def mainUrl = token_url
* def client_Url = openidclients_url

@ignore	
Scenario: Fetch all client token
	Given url mainUrl 
	When method GET 
	Then status 401 
	And print response

@ignore	
Scenario: Fetch all client token
	Given url client_Url
	And header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
	Given url mainUrl 
	And header Authorization = 'Bearer ' + accessToken
	And param clientId = response.entries[0].inum
    And print 'clientId = '+clientId 
	When method GET 
	Then status 200 
	And print response



	
