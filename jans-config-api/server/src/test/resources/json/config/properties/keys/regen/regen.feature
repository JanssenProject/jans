@ignore
Feature: Verify Key Regeneration Resource endpoint

  	Background:
  	* def mainUrl = keyRegenUrl

 	@key-regen-get
  	Scenario: Retrieve Key Regeneration configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    @key-regen-put
  	Scenario: Update Key Regeneration configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request read('regen.json')
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    
    @error
  	Scenario: Error case for keyRegenerationInterval configuration validation
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('regen.json') 
    Then set request_json.keyRegenerationInterval = 0
    #And print request_json
    And request request_json
    When method PUT
    Then status 400
    And print response

    