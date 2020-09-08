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
    
    @ignore
    @cache-put-CacheProviderType
  	Scenario: Update CacheProviderType configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
	When method GET
    Then status 200
    And print response
    And assert response.length != null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And print response.cacheProviderType
    And request ({cacheProviderType:response.cacheProviderType})
    #And request read('cacheProviderType.json')
    When method PUT
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
        
    @cache-get-in-memory
  	Scenario: Retrieve in-memory Cache configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And path 'in-memory'
    When method GET
    Then status 200
    And print response
    And assert response.length != null
        
    @cache-post-in-memory
  	Scenario: Add in-memory Cache configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And path 'in-memory'
    When method GET
    Then status 200
    Then print response
    Then def first_response = response 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And path 'in-memory'
    And request first_response 
    When method POST
    Then status 201
    And print response
    And assert response.length != null
        
    @cache-put-in-memory
  	Scenario: Update in-memory Cache configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And path 'in-memory'
    When method GET
    Then status 200
    Then print response
    Then def first_response = response 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And path 'in-memory'
    And request first_response 
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    
    @cache-get-native-persistence
  	Scenario: Retrieve native-persistence Cache configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And path 'native-persistence'
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    @cache-post-native-persistence
  	Scenario: Add native-persistence Cache configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And path 'native-persistence'
    When method GET
    Then status 200
    Then print response
    Then def first_response = response 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And path 'native-persistence'
    And request first_response 
    When method POST
    Then status 201
    And print response
    And assert response.length != null
    
    @cache-put-native-persistence
  	Scenario: Update native-persistence Cache configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And path 'native-persistence'
    When method GET
    Then status 200
    Then print response
    Then def first_response = response 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And path 'native-persistence'
    And request first_response 
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    
	@cache-get-memcached
  	Scenario: Retrieve memcached Cache configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And path 'memcached'
    When method GET
    Then status 200
    And print response
    And assert response.length != null    
    
    @cache-post-memcached
  	Scenario: Add memcached Cache configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And path 'memcached'
    When method GET
    Then status 200
    Then print response
    Then def first_response = response 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And path 'memcached'
    And request first_response 
    When method POST
    Then status 201
    And print response
    And assert response.length != null
    
    @cache-put-memcached
  	Scenario: Update memcached Cache configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And path 'memcached'
    When method GET
    Then status 200
    Then print response
    Then def first_response = response 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And path 'memcached'
    And request first_response 
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    