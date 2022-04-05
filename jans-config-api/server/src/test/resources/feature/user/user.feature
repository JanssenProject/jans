
Feature: User endpoint

    Background:
    * def mainUrl = user_url

Scenario: Fetch all user without bearer token
Given url mainUrl
When method GET
Then status 401


Scenario: Fetch all user
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response
And assert response.length != null


Scenario: Fetch the first three users
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And param limit = 3
When method GET
Then status 200
And print response
And assert response.length == 3

Scenario: Get an user by inum(unexisting user)
Given url mainUrl + '/53553532727272772'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404


Scenario: Delete a non-existion user by inum
Given url mainUrl + '/1402.66633-8675-473e-a749'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404
And print response


