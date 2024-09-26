
Feature: Token flow

Background:
* def mainUrl = token_url

@ignore	
Scenario: Fetch all client token
Given url mainUrl 
When method GET 
Then status 401 
And print response


Scenario: Fetch all token
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
Given url mainUrl + '/search'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response


	
