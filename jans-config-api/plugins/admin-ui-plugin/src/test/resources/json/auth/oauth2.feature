Feature: Oauth2

  Background:
    * def ujwt = 'eyJraWQiOiI2NmQ1ODk4Ny03NjQ0LTRkYjEtOWU2YS04ZmFiNWFjYWVhNjVfc2lnX3JzMjU2IiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJndFZ2cDJCMGlwdFN5bXFSTEpzc2ZZOWZNbjliWVZRNF8xVTBHSFlFWFRJIiwiYXVkIjoiMjA3M2M4ZTctMTA2MC00YzA5LWIwZjYtN2Q0NmZiNmZmOGY1IiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsIm5pY2tuYW1lIjoiQWRtaW4iLCJpc3MiOiJodHRwczovL2FkbWluLXVpLXRlc3QuZ2x1dS5vcmciLCJnaXZlbl9uYW1lIjoiQWRtaW4iLCJtaWRkbGVfbmFtZSI6IkFkbWluIiwiZmFtaWx5X25hbWUiOiJVc2VyIiwiamFuc0FkbWluVUlSb2xlIjpbImFwaS1hZG1pbiJdfQ.losuUsBib2YvB2t995iT0HJvE-q7uZT8zHrTJsWN8_rxB_-kawFNI6weWiH4hpAaIAIaw6Bnq5AczSW3OS6sn5fZDrBxuftrurCa7PK7uAeYim8Zozg0NHNQQ9FDe6MYg8FLtKI3eiusvgC3P4CClIFf1AMGVxREyBra87r_J8j2IyV86Ktjv_rVZLNm2mChOrbM5sIjaski4saKtZTMiVZoK7WMC4FJmS8ttysfG2w7-t8MoI9kiM890RhxSoUba9nQIVxKmpdJOzam8_FqNfQmC9fzI2XKjgRu16SoAoJxCNeTy8HHBCN4H_BwxgU2seSbDTMgU11GiApd4D2xaw'

  Scenario: Testing oauth2 GET configuration endpoint
    Given url adminUIConfigURL
    And header Accept = 'application/json'
    When method GET
    Then status 200
    And print response

  Scenario: Testing api-protection-token GET endpoint
    Given url apiProtectionTokenURL
    And header Accept = 'application/json'
    Given param ujwt = ujwt
    When method GET
    Then status 200
    And print response


#Scenario: Testing access-token GET endpoint
  #Given url https://admin-ui-test.gluu.org/jans-config-api/admin-ui/oauth2/access-token
  #When method GET
  #Then status 200
  #And print response