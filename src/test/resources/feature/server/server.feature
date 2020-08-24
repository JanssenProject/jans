Feature: Verify Server configuration endpoint

  	Background:
  	* def mainUrl = serverUrl

 	@server-get
  	Scenario: Retrieve Server configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    @server-put
  	Scenario: Update Server configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request read('server.json')
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    
    @error
  	Scenario: Error case for oxId configuration validation
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('server.json') 
    Then set request_json.oxId = null
    #And print request_json
    And request request_json
    When method PUT
    Then status 400
    And print response
    
    @error
  	Scenario: Error case for configurationUpdateInterval configuration validation
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('server.json') 
    Then set request_json.configurationUpdateInterval = 0
     #And print request_json
    And request request_json
    When method PUT
    Then status 400
    And print response
     
    