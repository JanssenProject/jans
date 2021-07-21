@ignore
Feature: Statistics 

Background:
* def mainUrl = statUrl

Scenario: Fetch all statistics without bearer token 
	Given url mainUrl + '/user'
    And param month = '202101'     
	When method GET 
	Then status 401 

Scenario: Fetch user statistics
	Given url mainUrl + '/user'
    And header Authorization = 'Bearer ' + accessToken
    And param month = '202101' 
    And param format = 'openmetrics' 
	When method GET 
	Then status 200 
	And print response



