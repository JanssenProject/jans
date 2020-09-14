Feature: JWKS endpoint

  Background:
    * def jwksUrl = baseUrl + '/api/v1/oxauth/config/jwks'

  Scenario: Retrieve JWKS
    Given url  jwksUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null