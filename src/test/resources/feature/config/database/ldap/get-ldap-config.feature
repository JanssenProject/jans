
Feature: Verify LDAP configuration GET endpoint

  	Background:
  	* def mainUrl = ldapUrl
  	
   	@ldap-config-get
  	Scenario: Retrieve LDAP configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
   