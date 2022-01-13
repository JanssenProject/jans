@ignore
Feature: Verify UmaConfiguration endpoint

  	Background:
  	* def mainUrl = umaConfigurationUrl

 	@uma-get
  	Scenario: Retrieve UmaConfiguration configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    
    @uma-put
  	Scenario: Update UmaConfiguration configuration
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
    
    @uma-error
  	Scenario: umaConfigurationEndpoint configuration cannot be null or empty
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response
    Then set result.umaConfigurationEndpoint = null 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    @uma-error
  	Scenario: umaRptLifetime configuration cannot be less than 1 (one)
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response
    Then set result.umaRptLifetime = 0 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    @uma-error
  	Scenario: umaTicketLifetime configuration cannot be less than 1 (one)
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response
    Then set result.umaTicketLifetime = -100
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    @uma-error
  	Scenario: umaPctLifetime configuration cannot be less than 1 (one)
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response
    Then set result.umaPctLifetime = 0
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    @uma-error
  	Scenario: umaResourceLifetime configuration cannot be less than 1 (one)
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response
    Then set result.umaResourceLifetime = 0
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
   