
Feature: Verify Server stats

Background:
  * def mainUrl = healthUrl + "/server-stat"
  
  Scenario: Verify Underlying server stats
  Given url mainUrl
  When method GET
  Then status 200
  And print response

  
   