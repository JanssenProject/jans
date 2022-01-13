@ignore
Feature: Verify Subject configuration endpoint

  	Background:
  	* def mainUrl = subjectUrl

 	@subject-get
  	Scenario: Retrieve Subject configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    @subject-put
  	Scenario: Update Subject configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request read('subject.json')
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    
    @error
  	Scenario: Error case for subjectTypesSupported configuration validation
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('subject.json') 
    Then set request_json.subjectTypesSupported = null
    And print request_json
    And request request_json
    When method PUT
    Then status 400
    And print response
    
   