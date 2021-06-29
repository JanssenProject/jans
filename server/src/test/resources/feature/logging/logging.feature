
Feature: Logging connection configuration

  Background:
    * def mainUrl = logging_url
    
    
  Scenario: Retrieve logging configuration without bearer token
    Given url  mainUrl
    When method GET
    Then status 401
    And print response


  Scenario: Retrieve logging configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null


  Scenario: Update logging configuration
   Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def first_response = response 
    And header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 200
    And print response
    