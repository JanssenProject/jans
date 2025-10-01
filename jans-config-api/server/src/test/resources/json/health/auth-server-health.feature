
Feature: Verify API HealthCheck

Background:
* def mainUrl = auth_health_url
  
  Scenario: Verify all stats of the health
  Given url mainUrl
  When method GET
  Then status 200
  And print response
  
 