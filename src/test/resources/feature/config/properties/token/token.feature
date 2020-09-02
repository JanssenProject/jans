@ignore
Feature: Verify Token configuration endpoint

  	Background:
  	* def mainUrl = tokenUrl

 	@token-get
  	Scenario: Retrieve Token configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    @token-put
  	Scenario: Update Token configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request read('token.json')
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    
    @error
  	Scenario: Error case for authorizationCodeLifetime configuration validation
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('token.json') 
    Then set request_json.authorizationCodeLifetime = 0
    And print request_json
    And request request_json
    When method PUT
    Then status 400
    And print response
    
    @error
  	Scenario: Error case for refreshTokenLifetime configuration validation
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('token.json') 
    Then set request_json.refreshTokenLifetime = 0
    And print request_json
    And request request_json
    When method PUT
    Then status 400
    And print response
    
    @error
  	Scenario: Error case for accessTokenLifetime configuration validation
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('token.json') 
    Then set request_json.accessTokenLifetime = 0
    And print request_json
    And request request_json
    When method PUT
    Then status 400
    And print response
   