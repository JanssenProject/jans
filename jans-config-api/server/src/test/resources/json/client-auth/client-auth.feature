
Feature: Client Authorizations 

Background:
* def mainUrl = clients_authorizations_url

Scenario: Fetch all clients authorizations without bearer token 
	Given url mainUrl + '/123'
	When method GET 
	Then status 401 


Scenario: Fetch all clients authorizations
	Given url mainUrl + '/123'
	And print 'accessToken = '+accessToken
	And print 'issuer = '+issuer
	And header Authorization = 'Bearer ' + accessToken
	#And header issuer = issuer  
	When method GET 
	Then status 200 
	And print response
	#And assert response.length != null 
	#And assert response.length >= 10 

