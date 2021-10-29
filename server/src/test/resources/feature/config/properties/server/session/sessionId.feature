@ignore
Feature: Verify SessionId configuration endpoint

  	Background:
  	* def mainUrl = sessionIdUrl

 	@sessionid-get
  	Scenario: Retrieve SessionId configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    @sessionid-put
  	Scenario: Update SessionId configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 200
    And print response
    
    @sessionid-error
  	Scenario: sessionIdUnusedLifetime configuration cannot be less than 1 (one)
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response
    Then set result.sessionIdUnusedLifetime = -8 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    @sessionid-error
  	Scenario: sessionIdUnusedLifetime configuration cannot be less than 1 (one)
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response
    Then set result.sessionIdUnauthenticatedUnusedLifetime = -2 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    @sessionid-error
  	Scenario: sessionIdLifetime configuration cannot be less than -1 (minus one)
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response
    Then set result.sessionIdUnauthenticatedUnusedLifetime = -3 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    @sessionid-error
  	Scenario: serverSessionIdLifetime configuration cannot be less than -1 (minus one)
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response
    Then set result.serverSessionIdLifetime = -5 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    