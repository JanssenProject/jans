Feature: Verify Cache configuration endpoint

  	Background:
  	* def mainUrl = cacheUrl

 	@cache-get
  	Scenario: Retrieve Cache configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    @cache-get-redis
  	Scenario: Retrieve Redis Cache configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And path 'redis'
    When method GET
    Then status 200
    And print response
    And assert response.length != null

    @cache-post-redis
  	Scenario: Add Redis Cache configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And path 'redis'
    When method GET
    Then status 200
    Then print response
    Then def first_response = response 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And path 'redis'
    And request first_response 
    When method POST
    Then status 201
    And print response
    And assert response.length != null
   
    @cache-put-redis
  	Scenario: Update Redis Cache configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And path 'redis'
    When method GET
    Then status 200
    Then print response
    Then def first_response = response 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And path 'redis'
    And request first_response 
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    
    