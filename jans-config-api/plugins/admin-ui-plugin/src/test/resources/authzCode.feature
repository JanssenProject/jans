@ignore
Feature: This Feature is to get authorization code to test the test cases - Do not remove ignore tag

Background:
* def loginurl =  authzurl;
* def grantType =  testProps.get('test.grant.type');
* def clientId =  testProps.get('test.client.id');
* def clientSecret =  testProps.get('test.client.secret');
* def scopes =  testProps.get('test.scopes');

* def authStr = clientId+':'+clientSecret
* def Base64 = Java.type('java.util.Base64')
* def encodedGrantType = java.net.URLDecoder.decode(grantType, 'UTF-8')
* def encodedScopes = java.net.URLDecoder.decode(scopes, 'UTF-8')


Scenario: Get code
  Given url loginurl
  And print 'authzurl = '+loginurl
  And print 'grantType = '+grantType
  And print 'clientId = '+clientId
  And print 'clientSecret = '+clientSecret
  And print 'scopes = '+scopes
  And print 'encodedGrantType = '+encodedGrantType
  And print 'encodedScopes = '+encodedScopes
  And header Accept = 'application/json'
  Given param grant_type = grantType
  And param scope = scopes
  And param client_id = clientId
  And param response_type = 'code'
  And param redirect_uri = 'https://admin-ui-test.gluu.org/admin'
  When method GET
  Then status 200
  And print 'response = '+response

