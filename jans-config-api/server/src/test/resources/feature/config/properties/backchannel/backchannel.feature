@ignore
Feature: Verify backchannel configuration endpoint

Background:
  * def mainUrl = backchannelUrl
   
  @backchannel-get
  Scenario: Retrieve backchannel configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null

   
   @backchannel-put
   Scenario: Update backchannel configuration
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
   Scenario: Error case while updating backchannel configuration min validation
   Given url  mainUrl
   And  header Authorization = 'Bearer ' + accessToken
   When method GET
   Then status 200
   Then def first_response = response 
   Then set first_response.backchannelAuthenticationResponseExpiresIn = 0
   Given url mainUrl
   And  header Authorization = 'Bearer ' + accessToken
   And request first_response
   When method PUT
   Then status 400
   And print response

   