
Feature: Verify SAML Trust Relationship endpoint

	Background:samlUrl
  	* def mainUrl = samlTrustRelationshipUrl

  	@get-trust-relationship-no-token
	Scenario: Fetch SAML Trust Relationships without bearer token 
	Given url mainUrl 
	When method GET 
	Then status 401 
	
 	@get-trust-relationship
  	Scenario: Retrieve SAML Trust Relationship
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null

