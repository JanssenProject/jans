@ignore
Feature: Openid connect Scopes

Background:
* def mainUrl = scopes_url
* def getToken =
"""
function(path,method) {
print(' path = '+path+' , method = '+method);
var result = karate.call('classpath:token.feature',{ pathUrl: path, methodName: method});
print(' result.response after call = '+result.response);
var token = result.response
  return token;
}
"""   

Scenario: Fetch all openid connect scopes without bearer token
Given url mainUrl
When method GET
Then status 401


Scenario: Fetch all scopes
Given url mainUrl
#And def accessToken = call read('classpath:token.feature') { pathUrl: #(mainUrl), methodName: 'GET' }
And def accessToken = getToken(mainUrl,'GET')
And print accessToken
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response
And assert response.length != null


@ignore
Scenario: Fetch all openid connect scopes
Given url mainUrl
And  header Authorization = 'Bearer ' + accessToken
And param type = 'openid'
When method GET
Then status 200
And print response
And assert response.length != null

@ignore
Scenario: Fetch the first three openidconnect scopes
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And param type = 'openid'
And param limit = 3
When method GET
Then status 200
And print response
And assert response.length == 3

@ignore
Scenario: Search openid connect scopes given a serach pattern
Given url mainUrl
And  header Authorization = 'Bearer ' + accessToken
And param type = 'openid'
And param pattern = 'openid'
When method GET
Then status 200
And print response
And assert response.length == 1

@ignore
@CreateUpdateDelete
Scenario: Create new OpenId Connect Scope
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And request read('classpath:scope.json')
When method POST
Then status 201
Then def result = response
Then set result.displayName = 'UpdatedQAAddedScope'
Then def inum_before = result.inum
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And request result
When method PUT
Then status 200
And assert response.displayName == 'UpdatedQAAddedScope'
And assert response.inum == inum_before
Given url mainUrl + '/' +response.inum
And header Authorization = 'Bearer ' + accessToken
When method DELETE
Then status 204

@ignore
Scenario: Delete a non-existing openid connect scope by inum
Given url mainUrl + '/1402.66633-8675-473e-a749'
And header Authorization = 'Bearer ' + accessToken
And param type = 'openid'
When method GET
Then status 404
And print response

@ignore
Scenario: Get an openid connect scope by inum(unexisting scope)
Given url mainUrl + '/53553532727272772'
And header Authorization = 'Bearer ' + accessToken
And param type = 'openid'
When method GET
Then status 404
And print response

@ignore
Scenario: Get an openid connect scopes by inum
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And param type = 'openid'
When method GET
Then status 200
Given url mainUrl + '/' +response[0].inum
And header Authorization = 'Bearer ' + accessToken
And param type = 'openid'
When method GET
Then status 200