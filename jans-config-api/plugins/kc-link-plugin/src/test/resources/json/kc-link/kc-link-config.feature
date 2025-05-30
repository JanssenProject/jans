@ignore
Feature: Verify KC Link configuration endpoint

	Background:kcLinkUrl
  	* def mainUrl = kcLinkUrl
  	
 	@kc-link-config-get
  	Scenario: Retrieve KC Link configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null


    @kc-link-config-put
  	Scenario: Update KC Link configuration
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
           

    @kc-link-config-get-error
    Scenario: Retrieve KC Link configuration without bearer token
    Given url  mainUrl
    When method GET
    Then status 401
    And print response
 
   
   