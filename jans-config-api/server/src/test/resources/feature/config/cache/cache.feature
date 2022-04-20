
Feature: Verify Cache configuration endpoint

  	Background:
  	* def mainUrl = cacheUrl
  	
    @cache-get-error
    Scenario: Retrieve Cache configuration without bearer token
    Given url  mainUrl
    When method GET
    Then status 401
    And print response
  
 	@cache-get
  	Scenario: Retrieve Cache configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    @cache-patch
	Scenario: Patch cacheProviderType configuration
	Given url  mainUrl
	And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    And print response.cacheProviderType
  	Given url  mainUrl
  	And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request "[ {\"op\":\"replace\", \"path\": \"/cacheProviderType\", \"value\":\""+response.cacheProviderType+"\" } ]"
	Then print request
    When method PATCH
    Then status 200
    And print response
    
    @cache-patch
	Scenario: Patch nativePersistenceConfiguration configuration
	Given url  mainUrl
	And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    And print response.nativePersistenceConfiguration
  	Given url  mainUrl
  	And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request "[ {\"op\":\"replace\", \"path\": \"/nativePersistenceConfiguration\", \"value\":"+response.nativePersistenceConfiguration+" } ]"
	Then print request
    When method PATCH
    Then status 200
    And print response
    
    @cache-patch
	Scenario: Patch inMemoryConfiguration configuration
	Given url  mainUrl
	And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    And print response.inMemoryConfiguration
  	Given url  mainUrl
  	And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request "[ {\"op\":\"replace\", \"path\": \"/inMemoryConfiguration\", \"value\":"+response.inMemoryConfiguration+" } ]"
	Then print request
    When method PATCH
    Then status 200
    And print response
    
    @cache-patch
	Scenario: Patch redisConfiguration configuration
	Given url  mainUrl
	And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    And print response.redisConfiguration
  	Given url  mainUrl
  	And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request "[ {\"op\":\"replace\", \"path\": \"/redisConfiguration\", \"value\":"+response.redisConfiguration+" } ]"
	Then print request
    When method PATCH
    Then status 200
    And print response
    
    @cache-patch
	Scenario: Patch memcachedConfiguration configuration
	Given url  mainUrl
	And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    And print response.memcachedConfiguration
  	Given url  mainUrl
  	And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request "[ {\"op\":\"replace\", \"path\": \"/memcachedConfiguration\", \"value\":"+response.memcachedConfiguration+" } ]"
	Then print request
    When method PATCH
    Then status 200
    And print response
    
    @cache-get-redis
  	Scenario: Retrieve Redis Cache configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And path 'redis'
    When method GET
    Then status 200
    And print response
    And assert response.length != null

    @cache-put-redis
  	Scenario: Update Redis Cache configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And path 'redis'
    When method GET
    Then status 200
    Then print response
    Then def first_response = response 
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And path 'redis'
    And request first_response 
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    
    @cache-patch-redis
	Scenario: Patch redis configuration
	Given url  mainUrl
	And header Authorization = 'Bearer ' + accessToken
    And path 'redis'
    When method GET
    Then status 200
    And print response
    And assert response.length != null
  	Given url  mainUrl
  	And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request "[ {\"op\":\"replace\", \"path\": \"/defaultPutExpiration\", \"value\":"+response.defaultPutExpiration+"} ]"
    And path 'redis'
	Then print request
    When method PATCH
    Then status 200
    And print response
        
    @cache-get-in-memory
  	Scenario: Retrieve in-memory Cache configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And path 'in-memory'
    When method GET
    Then status 200
    And print response
    And assert response.length != null        
        
    @cache-put-in-memory
  	Scenario: Update in-memory Cache configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And path 'in-memory'
    When method GET
    Then status 200
    Then print response
    Then def first_response = response 
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And path 'in-memory'
    And request first_response 
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    
    @cache-patch-in-memory
	Scenario: Patch in-memory configuration
	Given url  mainUrl
	And header Authorization = 'Bearer ' + accessToken
    And path 'in-memory'
    When method GET
    Then status 200
    And print response
    And assert response.length != null
  	Given url  mainUrl
  	And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request "[ {\"op\":\"replace\", \"path\": \"/defaultPutExpiration\", \"value\":"+response.defaultPutExpiration+" } ]"
    And path 'in-memory'
	Then print request
    When method PATCH
    Then status 200
    And print response
    
    @cache-get-native-persistence
  	Scenario: Retrieve native-persistence Cache configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And path 'native-persistence'
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    @cache-put-native-persistence
  	Scenario: Update native-persistence Cache configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And path 'native-persistence'
    When method GET
    Then status 200
    Then print response
    Then def first_response = response 
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And path 'native-persistence'
    And request first_response 
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    
    @cache-patch-native-persistence
	Scenario: Patch native-persistence configuration
	Given url  mainUrl
	And header Authorization = 'Bearer ' + accessToken
    And path 'native-persistence'
    When method GET
    Then status 200
    And print response
    And assert response.length != null
  	Given url  mainUrl
  	And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request "[ {\"op\":\"replace\", \"path\": \"/defaultPutExpiration\", \"value\":"+response.defaultPutExpiration+"} ]"
    And path 'native-persistence'
	Then print request
    When method PATCH
    Then status 200
    And print response
    
	@cache-get-memcached
  	Scenario: Retrieve memcached Cache configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And path 'memcached'
    When method GET
    Then status 200
    And print response
    And assert response.length != null    
    
    @cache-put-memcached
  	Scenario: Update memcached Cache configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And path 'memcached'
    When method GET
    Then status 200
    Then print response
    Then def first_response = response 
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And path 'memcached'
    And request first_response 
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    

    @cache-patch-memcached
	Scenario: Patch memcached configuration
	Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And path 'memcached'
    When method GET
    Then status 200
    And print response
    And assert response.length != null
  	Given url  mainUrl
  	And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request "[ {\"op\":\"replace\", \"path\": \"/defaultPutExpiration\", \"value\":"+response.defaultPutExpiration+"} ]"
    And path 'memcached'
	Then print request
    When method PATCH
    Then status 200
    And print response
    