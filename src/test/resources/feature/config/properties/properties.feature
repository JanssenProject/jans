Feature: Verify Auth configuration endpoint

  	Background:
  	* def mainUrl = authConfigurationUrl

 	@auth-config-get
  	Scenario: Retrieve Auth configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    @auth-config-patch
  	Scenario: Patch cibaEnabled Auth configuration
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Given path 'cibaEnabled'
    And request {'cibaEnabled':true}
	Then print request
    When method PATCH
    Then status 200
    And print response
    
     @auth-config-patch
  	Scenario: Patch clientBlackList Auth configuration
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Given path 'clientBlackList'
    And request {'clientBlackList':['/*.attacker.com/*','/*.hackers.com/*']}
	Then print request
    When method PATCH
    Then status 200
    And print response
    
   