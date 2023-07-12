
Feature: Verify Jans Link configuration endpoint

	Background:jans-link
  	* def mainUrl = jansLinkUrl

	@ignore
	@jansLink-get-error
    Scenario: Retrieve Jans Link configuration without bearer token
    Given url  mainUrl
    When method GET
    Then status 401
    And print response
 
    @ignore
 	@jansLink-get
  	Scenario: Retrieve Jans Link configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null

	@ignore
    @jansLink-put
  	Scenario: Update Jans Link configuration
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
           


   
   