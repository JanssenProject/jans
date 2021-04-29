Feature: Verify Custom Script configuration endpoint

	Background:
  	* def mainUrl = scriptsUrl
  	
  	
  	@scripts-get
    Scenario: Retrieve Custom Script configuration without bearer token
    Given url  mainUrl
    When method GET
    Then status 401
    And print response
  	
  	
 	@scripts-get
  	Scenario: Retrieve Custom Script configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null


   	@scripts-get-person-custom-scripts
	Scenario: Fetch all person custom scripts without bearer token
	Given url mainUrl + '/type'
	And path 'person_authentication'
	And header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
	And assert response.length != null


    @scripts-get-introspection-custom-scripts
	Scenario: Fetch all introspection scripts without bearer token
	Given url mainUrl + '/type'
	And path 'introspection'
	And header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
	And assert response.length != null
	
	
	