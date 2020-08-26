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
    And request read('expiration.json')
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    
    @error
  	Scenario: Error case for expirationNotificatorMapSizeLimit configuration validation
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('expiration.json') 
    Then set request_json.expirationNotificatorMapSizeLimit = 0
    #And print request_json
    And request request_json
    When method PUT
    Then status 400
    And print response
    
    @error
  	Scenario: Error case for expirationNotificatorIntervalInSeconds configuration validation
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('expiration.json') 
    Then set request_json.expirationNotificatorIntervalInSeconds = 0
     #And print request_json
    And request request_json
    When method PUT
    Then status 400
    And print response
     
    