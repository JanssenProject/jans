@ignore
Feature: JWKS endpoint

  Background:
    * def jwksUrl = baseUrl + '/api/v1/config/jwks'

  Scenario: Retrieve JWKS
    Given url  jwksUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null

  Scenario: Patch JWKS with new key
    Given url  jwksUrl
    And  header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request read('jwks_patch.json')
    Then print request
    When method PATCH
    Then status 200
    And print response

  Scenario: Put JWKS
    Given url  jwksUrl
    And  header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json'
    And header Accept = 'application/json'
    And request read('jwks.json')
    Then print request
    When method PUT
    Then status 200
    And print response
