
Feature: JWKS endpoint

	Background:
  	* def mainUrl = jwksUrl
  	
  Scenario: Retrieve JWKS without bearer token
    Given url  mainUrl
    When method GET
    Then status 401
    And print response


  Scenario: Retrieve JWKS
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null


   Scenario: Patch JWKS with new key
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
  	Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And print response.keys[0].exp
    And request "[ {\"op\":\"replace\", \"path\": \"/keys/0/exp\", \"value\":\""+response.keys[0].exp+"\" } ]"
	Then print request
    When method PATCH
    Then status 200
    And print response

  Scenario: Put JWKS
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then print response
    Then def first_response = response 
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And request first_response 
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    
  Scenario: POST, GET, PATCH and selete a Key
    #Given url mainUrl + '/test12345-66f1-4b4a-92ec-d969522f4cbc_sig_rs256'
    #And header Authorization = 'Bearer ' + accessToken
    #When method DELETE
    #Then status 204 
    Given url mainUrl + '/key'
    And header Authorization = 'Bearer ' + accessToken
    And request read('jwk_key.json')
    When method POST
    Then status 201
    And print response
    Given url mainUrl + '/' +response.kid
    And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    Given url mainUrl + '/' +response.kid
    And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And print response.exp
    And request "[ {\"op\":\"replace\", \"path\": \"/exp\", \"value\":\""+response.exp+"\" } ]"
    Then print request
    When method PATCH
    Then status 200
    And print response    
    Given url mainUrl + '/test12345-66f1-4b4a-92ec-d969522f4cbc_sig_rs256'
    And header Authorization = 'Bearer ' + accessToken
    When method DELETE
    Then status 204 
