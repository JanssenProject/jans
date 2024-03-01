@ignore
Feature: Verify Lock configuration endpoint

	Background:LockUrl
  	* def mainUrl = LockUrl
  	
 	@lock-config-get
  	Scenario: Retrieve Lock configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null


    @lock-config-put
  	Scenario: Update Lock configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request response
    When method PUT
    Then status 200
    And print response
           

    @lock-config-get-error
    Scenario: Retrieve Lock configuration without bearer token
    Given url  mainUrl
    When method GET
    Then status 401
    And print response
 
   
   