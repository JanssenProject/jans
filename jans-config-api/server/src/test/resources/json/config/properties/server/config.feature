@ignore
Feature: Verify Server config configuration endpoint

  	Background:
  	* def mainUrl = serverConfigUrl

 	@server-get
  	Scenario: Retrieve Server config configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    @server-put
  	Scenario: Update Server config configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    #And print response
    Then def first_response = response 
    #And print first_response
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 200
    And print response
    
    @error
  	Scenario: Error case for oxId configuration validation
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then def first_response = response 
    Then set first_response.oxId = null
    Given url mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 400
    And print response
  	
    
    @error
  	Scenario: Error case for configurationUpdateInterval configuration validation
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then def first_response = response 
    Then set first_response.configurationUpdateInterval = 0
    Given url mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 400
    And print response
        
    