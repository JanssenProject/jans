@ignore
Feature: Verify API HealthCheck

Background:
  * def mainUrl = healthUrl
  * def health_schema = { name: '#string', status: '#string' }
  * def status_str = 'UP'
  * def response_str = [{"name": "jans-config-api liveness","status": "UP"},{"name": "jans-config-api readiness","status": "UP"}]
  * def live_str = [{"name": "jans-config-api liveness","status": "UP"}]
  * def ready_str = [{"name": "jans-config-api readiness","status": "UP"}]
  
  Scenario: Verify all stats of the health
  Given url mainUrl
  When method GET
  Then status 200
  And match response == 
     """
  { 

    "status": #string? _ == status_str,
    "checks": '#(health_schema)',
     checks: '#[2]'  
 }
  """
  
  
  Scenario: Verify liveness status of API
  Given url mainUrl + '/live/'
  When method GET
  Then status 200
  And match response == 
     """
  { 

    "status": #string? _ == status_str,
    "checks": '#(health_schema)',
     checks: '#[1]'  
 }
  """
    
  Scenario: Verify readiness status of API
  Given url mainUrl + '/ready/'
  When method GET
  Then status 200
   And match response == 
     """
  { 

    "status": #string? _ == status_str,
    "checks": '#(health_schema)',
     checks: '#[1]'  
 }
  """
  
  
   