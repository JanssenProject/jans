Feature: Allow authenticated users to access protected pages
    
    @authenticated
    Scenario: User is authenticated
        Given username is "johndoe"
            And user is authenticated
            And protected content link is https://chris.testingenv.org/protected-content
        When user clicks the protected content link
        Then user access the protected content link

    Scenario: User does not exist
        Given user does not exist
            And protected content link is https://chris.testingenv.org/protected-content
        When user clicks the protected content link
        Then user goes to external login page

    Scenario: User is not authenticated
        Given username is "johndoe"
            And protected content link is https://chris.testingenv.org/protected-content
        When user clicks the protected content link
        Then user goes to external login page

    # Scenario: Normal user try to access admin content
    #     Given username is "johndoe"
    #         And user role is "user"
    #         And protected content link is https://chris.testingenv.org/admin/admin-protected-content
    #     When user clicks the protected content link
    #     Then user gets a 403 error

    # Scenario: Admin can access admin contents
    #     Given username is "johndoe"
    #         And user role is "admin"
    #         And protected content link is https://chris.testingenv.org/admin/admin-protected-content
    #     When user clicks the protected content link
    #     Then user access the protected content link
    
    



