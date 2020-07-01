Feature: Metrics feature
Scenario: Testing the exact response of a GET endpoint
Given url metricsUrl
When method GET
Then status 200
And match $ contains {metricReporterEnabled:"#notnull"}