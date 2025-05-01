
Feature: Configure SMTP server

	Background:
  	* def mainUrl = smtp_url
 	    
    @get-smtp-config
  	Scenario: Get SMTP server details    
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken 
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    

	@CreateGetUpdateDelete
	Scenario: Setup SMTP configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken 
    When method GET
    Then status 200
    And print response
    And def smtpConf = (response.length != null ? response : read('smtp.json'))
    And print smtpConf
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken
	And request smtpConf
	When method POST
	Then status 201
	And print response
    Then def result = response
    Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken
	And request result
	When method PUT
	Then status 200
	And print response
    Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken
	When method DELETE
	Then status 204
	And print response
	
    @ignore   
    @test-smtp-config
    Scenario: Get SMTP server details    
    Given url  mainUrl +'/test'
    And header Authorization = 'Bearer ' + accessToken 
    And request read('smtp.json')
    When method POST
    Then status 200
    And print response
    And assert response.length != null
