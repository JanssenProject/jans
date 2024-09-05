
Feature: Verify Message configuration endpoint

  	Background:
  	* def mainUrl = messageUrl
  	
    @message-get-error
    Scenario: Retrieve Message configuration without bearer token
    Given url  mainUrl
    When method GET
    Then status 401
    And print response
  
 	@message-get
  	Scenario: Retrieve Message configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    @message-patch
	Scenario: Patch messageProviderType configuration
	Given url  mainUrl
	And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    And print response.messageProviderType
  	Given url  mainUrl
  	And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request "[ {\"op\":\"replace\", \"path\": \"/messageProviderType\", \"value\":\""+response.messageProviderType+"\" } ]"
	Then print request
    When method PATCH
    Then status 200
    And print response

    @message-patch
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
    
    @message-patch
	Scenario: Patch postgresConfiguration configuration
	Given url  mainUrl
	And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    And print response.postgresConfiguration
  	Given url  mainUrl
  	And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request "[ {\"op\":\"replace\", \"path\": \"/postgresConfiguration\", \"value\":"+response.postgresConfiguration+" } ]"
	Then print request
    When method PATCH
    Then status 200
    And print response
    
    @message-get-redis
  	Scenario: Retrieve Redis message configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And path 'redis'
    When method GET
    Then status 200
    And print response
    And assert response.length != null

    @message-put-redis
  	Scenario: Update Redis message configuration
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
    
    @message-patch-redis
	Scenario: Patch Redis message configuration
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
    And request "[ {\"op\":\"replace\", \"path\": \"/servers\", \"value\":\""+response.servers+"\"} ]"
    And path 'redis'
	Then print request
    When method PATCH
    Then status 200
    And print response
    
    @message-get-postgres
  	Scenario: Retrieve Postgres message configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And path 'postgres'
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    @message-put-postgres
  	Scenario: Update Postgres message configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And path 'postgres'
    When method GET
    Then status 200
    Then print response
    Then def first_response = response 
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And path 'postgres'
    And request first_response 
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    
    @message-patch-postgres
	Scenario: Patch Postgres message configuration
	Given url  mainUrl
	And header Authorization = 'Bearer ' + accessToken
    And path 'postgres'
    When method GET
    Then status 200
    And print response
    And assert response.length != null
  	Given url  mainUrl
  	And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request "[ {\"op\":\"replace\", \"path\": \"/dbSchemaName\", \"value\":\""+response.dbSchemaName+"\"} ]"
    And path 'postgres'
	Then print request
    When method PATCH
    Then status 200
    And print response

    