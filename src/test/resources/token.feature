@ignore
Feature: This Feature is to get token to test the test cases

Background:
* def mainUrl = test_url
* def getPath =
"""
function(path) {
print(' path = '+path);
path = path.replace(baseUrl,'');
print(' final path for token = '+path);
  return path;
}
"""

Scenario: Get Token
Given url mainUrl
And print url
And print pathUrl
And print methodName
And param method = methodName
And param path = getPath(pathUrl)
And print method
And print path
When method GET
Then status 200
And print 'token = '+response
