Feature: use passport social github to login
    """  
    As an user, 
    I want to use passport-social flow to authenticate
    So I can access protected-content
    """

    Background:
        Given auth method is passport-social
            And user is visiting "/"
    
    Scenario: User is authenticated
        Given username is "johndoe"
            And protected content link is https://localhost:5000/content/protected-user-content
        When user clicks the protected content link
        Then user access the protected content link

    Scenario: User is not authenticated
        Given user is not authenticated
        When user clicks the protected content link
        Then user goes to external login page


        
        
            