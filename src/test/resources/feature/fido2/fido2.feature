@ignore
Feature: Verify Fido2 configuration endpoint

	Background:
  	* def mainUrl = fido2Url
  	
 	@fido-get
  	Scenario: Retrieve Fido2 configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null

    @ignore
    @fido-put
  	Scenario: Update Fido2 configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    And print result.fido2Configuration.authenticationHistoryExpiration
    Then set result.fido2Configuration.authenticationHistoryExpiration = 800
    And print result.fido2Configuration.authenticationHistoryExpiration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 200
    And print response
           
    @ignore
    @fido-get-error
    Scenario: Retrieve Fido2 configuration without bearer token
    Given url  mainUrl
    When method GET
    Then status 401
    And print response
 
   
   