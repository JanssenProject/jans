
Feature: Verify Fido2 configuration endpoint

	Background:fido2Url
  	* def mainUrl = fido2Url

  	
 	@fido-get
  	Scenario: Retrieve Fido2 configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null


    @fido-put
  	Scenario: Update Fido2 configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request response
    When method PUT
    Then status 200
    And print response
           

    @fido-get-error
    Scenario: Retrieve Fido2 configuration without bearer token
    Given url  mainUrl
    When method GET
    Then status 401
    And print response
 
   
   