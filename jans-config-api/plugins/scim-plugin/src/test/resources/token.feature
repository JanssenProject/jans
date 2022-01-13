@ignore
Feature: This Feature is to get token to test the test cases - Do not remove ignore tag

Background:
* def mainUrl =  testProps.get('token.endpoint');
* def grantType =  testProps.get('token.grant.type');
* def clientId =  testProps.get('test.client.id');
* def clientSecret =  testProps.get('test.client.secret');
* def scopes =  testProps.get('test.scopes');
* def authStr = clientId+':'+clientSecret
* def Base64 = Java.type('java.util.Base64')
* def encodedAuth = Base64.encoder.encodeToString(authStr.bytes)
* def encodedScopes = java.net.URLDecoder.decode(scopes, 'UTF-8')


Scenario: Get Token
Given url mainUrl
And print 'mainUrl = '+mainUrl
And print 'grantType = '+grantType
And print 'clientId = '+clientId
And print 'clientSecret = '+clientSecret
And print 'scopes = '+scopes
And print 'authStr = '+authStr
And print 'encodedAuth = '+encodedAuth
And print 'encodedScopes = '+encodedScopes
And header Accept = 'application/json'
And header Authorization = 'Basic '+encodedAuth
And form field grant_type = grantType
And form field scope = scopes
When method POST
Then status 200
And print 'token response = '+response




#Scenario: Get Token
#Given url 'https://pujavs.jans.server/jans-auth/restv1/token'
#And header Accept = 'application/json'
#And header Authorization = 'Basic MTgwMi45ZGNkOThhZC1mZTJjLTRmZDktYjcxNy1kOTQzNmQ5ZjIwMDk6dGVzdDEyMzQ='
#And form field grant_type = 'client_credentials'
#And form field scope = 'https://jans.io/oauth/config/openid/clients.readonly'
#When method POST
#Then status 200
#And print 'token response = '+response
