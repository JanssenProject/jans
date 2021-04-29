@ignore
Feature: Verify Expiration Notificator configuration endpoint

  	Background:
  	* def mainUrl = expirationUrl

 	@expiration-get
  	Scenario: Retrieve Expiration Notificator configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    @expiration-put
  	Scenario: Update Expiration Notificator configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    #And print response
    Then def first_response = response 
    #And print first_response
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 200
    And print response
          
    @error
  	Scenario: Error case for expirationNotificatorMapSizeLimit configuration validation
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then def first_response = response 
    Then set first_response.expirationNotificatorMapSizeLimit = 0
    Given url mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 400
    And print response
    
    @error
  	Scenario: Error case for expirationNotificatorIntervalInSeconds configuration validation
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then def first_response = response 
    Then set first_response.expirationNotificatorIntervalInSeconds = 0
    Given url mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 400
    And print response
    