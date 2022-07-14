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

   	@scripts-get-custom-script-by-name
	Scenario: Fetch all custom scripts by name
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    And print response[0]
	And print 'Script inum = '+response[0].name
    And assert response[0].name != null
	And print  'Script Name = '+response[0].name
	And print 'Fetching script by name' + '-' +response[0].name
	Given url mainUrl + '/name' + '/'+response[0].name
	And header Authorization = 'Bearer ' + accessToken
	When method GET
    Then status 200
    And print response
    And assert response.length != null
	
   	@scripts-get-person-custom-scripts
	Scenario: Fetch all person custom script 
	Given url mainUrl + '/type'
	And path 'person_authentication'
	And header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
	And assert response.length != null


    @scripts-get-introspection-custom-scripts
	Scenario: Fetch all introspection scripts 
	Given url mainUrl + '/type'
	And path 'introspection'
	And header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
	And assert response.length != null
	
	
	Scenario: Patch person custom script by inum
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    And print response[0]
	And print 'Script inum = '+response[0].inum
    And assert response[0].inum != null
	And print  'Script Type = '+response[0].scriptType
	And print 'Patching script ' + '-' +response[0].scriptType + '-' +response[0].inum
	Given url mainUrl + '/'+response[0].inum
	And header Authorization = 'Bearer ' + accessToken
	And header Content-Type = 'application/json-patch+json'
	And def request_body = "[ {\"op\":\"replace\", \"path\": \"/enabled\", \"value\":"+response[0].enabled+" } ]"
	And print 'request_body ='+request_body
	And request request_body
	When method PATCH
	Then status 200
	And print response
	And assert response.length !=0	