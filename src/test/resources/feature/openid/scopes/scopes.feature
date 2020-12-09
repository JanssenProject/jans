@ignore
Feature: Openid connect Scopes

Scenario: Fetch all openid connect scopes without bearer token
Given url scopes_url
When method GET
Then status 401

Scenario: Fetch all scopes
Given url scopes_url
And  header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response
And assert response.length != null

Scenario: Fetch all openid connect scopes
Given url scopes_url
And  header Authorization = 'Bearer ' + accessToken
And param type = 'openid'
When method GET
Then status 200
And print response
And assert response.length != null

Scenario: Fetch the first three openidconnect scopes
Given url scopes_url
And  header Authorization = 'Bearer ' + accessToken
And param type = 'openid'
And param limit = 3
When method GET
Then status 200
And print response
And assert response.length == 3

Scenario: Search openid connect scopes given a serach pattern
Given url scopes_url
And  header Authorization = 'Bearer ' + accessToken
And param type = 'openid'
And param pattern = 'openid'
When method GET
Then status 200
And print response
And assert response.length == 1

@CreateUpdateDelete
Scenario: Create new OpenId Connect Scope
Given url scopes_url
And header Authorization = 'Bearer ' + accessToken
And request read('classpath:scope.json')
When method POST
Then status 201
Then def result = response
Then set result.displayName = 'UpdatedQAAddedScope'
Then def inum_before = result.inum
Given url scopes_url
And header Authorization = 'Bearer ' + accessToken
And request result
When method PUT
Then status 200
And assert response.displayName == 'UpdatedQAAddedScope'
And assert response.inum == inum_before
Given url scopes_url + '/' +response.inum
And header Authorization = 'Bearer ' + accessToken
When method DELETE
Then status 204

Scenario: Delete a non-existing openid connect scope by inum
Given url scopes_url + '/1402.66633-8675-473e-a749'
And header Authorization = 'Bearer ' + accessToken
And param type = 'openid'
When method GET
Then status 404
And print response


Scenario: Get an openid connect scope by inum(unexisting scope)
Given url scopes_url + '/53553532727272772'
And header Authorization = 'Bearer ' + accessToken
And param type = 'openid'
When method GET
Then status 404
And print response

Scenario: Get an openid connect scopes by inum
Given url scopes_url
And header Authorization = 'Bearer ' + accessToken
And param type = 'openid'
When method GET
Then status 200
Given url scopes_url + '/' +response[0].inum
And header Authorization = 'Bearer ' + accessToken
And param type = 'openid'
When method GET
Then status 200