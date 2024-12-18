# new feature
# Tags: optional

Feature: Test License endpoints of admin-ui

  Scenario Outline: Testing 'auditLogging' POST endpoint
    Given url <expression>
    And header <name> = <values>
    And request <body>
    When method <method>
    Then status <status>
    And print <exps>
    Examples:
      | expression         | name   | values             | body                                                      | method | status | exps     |
      | getAuditLoggingURL | Accept | 'application/json' | {\"message\": {\"childMsg\": \"Testing audit logging.\"}} | POST   | 200    | response |