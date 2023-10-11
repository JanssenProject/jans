
Feature: Verify SAML configuration endpoint

	Background:samlUrl
  	* def mainUrl = samlConfigUrl

  	@get-saml-config-no-token
	Scenario: Fetch SAML config without bearer token 
	Given url mainUrl 
	When method GET 
	Then status 401 
	
 	@get-saml-config
  	Scenario: Retrieve SAML configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null

