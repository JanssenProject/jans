Feature: Verify API HealthCheck

Background:
  * def mainUrl = healthUrl
  * def status_str = 'UP'
  * def response_str = [{"name": "oxauth-config-api liveness","status": "UP"},{"name": "oxauth-config-api readiness","status": "UP"}]
  * def live_str = [{"name": "oxauth-config-api liveness","status": "UP"}]
  * def ready_str = [{"name": "oxauth-config-api readiness","status": "UP"}]
  
  Scenario: Verify all stats of the health
  Given url mainUrl
  When method GET
  Then status 200
  And match response.status == status_str
  And def all_status = response.checks
  And match response_str == all_status 
  #And print response_str
  
  
  Scenario: Verify liveness status of API
  Given url mainUrl + '/live/'
  When method GET
  Then status 200
  And match response.status == status_str
  And def live_status = response.checks
  And match live_str == live_status 
    
  Scenario: Verify readiness status of API
  Given url mainUrl + '/ready/'
  When method GET
  Then status 200
  And match response.status == status_str
  And def ready_status = response.checks
  And match ready_str == ready_status 
  
   