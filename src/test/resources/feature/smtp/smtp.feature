@ignore
Feature: Configure STMP server

	Background:
  	* def mainUrl = smtp_url
 	    
    @get-smtp-config
  	Scenario: Get STMP server details    
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken 
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
 	    
    @post-smtp-config
  	Scenario: Get STMP server details    
    Given url  mainUrl
	And header Authorization = 'Bearer ' + accessToken
	And request read('smtp.json')
	When method POST
	Then status 201
	And print response
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 200 
	And print response
    
    @ignore
    @test-smtp-config
  	Scenario: Get STMP server details    
    Given url  mainUrl +'/test'
    And header Authorization = 'Bearer ' + accessToken 
    And request read('smtp.json')
    When method POST
    Then status 200
    And print response
    And assert response.length != null
    

	@CreateGetUpdateDelete
	Scenario: Setup stmp configuration
	Given url smtp_url
	And header Authorization = 'Bearer ' + accessToken
	And request read('smtp.json')
	When method POST
	Then status 201
	And print response
	Given url smtp_url
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 200 
	And print response
	#And assert response.fromEmailAddress == 'test@gmail.com'
	And assert response.host == 'smtp.gmail.com'
	Then def result = response
	#Then set result.fromEmailAddress = 'gluuqa@gmail.com'
	Given url smtp_url
	And header Authorization = 'Bearer ' + accessToken
	And request result
	When method PUT
	Then status 200
	And print response
	#And assert response.fromEmailAddress == 'gluuqa@gmail.com'
	Given url smtp_url
	And header Authorization = 'Bearer ' + accessToken
	When method DELETE
	Then status 204
	And print response
