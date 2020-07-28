Feature: SessionId feature
Scenario: Testing the exact response of a GET endpoint
Given url sessionidUrl
And  header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And match $ contains {sessionIdUnusedLifetime:"#notnull"}