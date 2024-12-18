@ignore
Feature: Verify Server cleanup configuration endpoint

  	Background:
  	* def mainUrl = serverCleanupUrl

 	@server-get
  	Scenario: Retrieve Server cleanup configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    @server-put
  	Scenario: Update Server cleanup configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request read('cleanup.json')
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    
    @error
  	Scenario: Error case for cleanServiceInterval configuration validation
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('cleanup.json') 
    Then set request_json.cleanServiceInterval = 0
    #And print request_json
    And request request_json
    When method PUT
    Then status 400
    And print response
    
    @error
  	Scenario: Error case for cleanServiceBatchChunkSize configuration validation
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('cleanup.json') 
    Then set request_json.cleanServiceBatchChunkSize = 0
     #And print request_json
    And request request_json
    When method PUT
    Then status 400
    And print response
     
    