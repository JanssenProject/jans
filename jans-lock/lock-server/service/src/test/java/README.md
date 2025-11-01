# Lock Server Service Tests

This directory contains comprehensive unit tests for the Lock Server service layer.

## Test Coverage

### Service Tests

- **AuditServiceTest** - Tests for audit entry persistence (log, telemetry, health)
- **TokenEndpointServiceTest** - Tests for OAuth token management and encryption

### Model Tests

- **AuditEndpointTypeTest** - Tests for audit endpoint type enumeration
- **AppConfigurationTest** - Tests for application configuration (protection mode, audit persistence mode)
- **LockProtectionModeTest** - Tests for lock protection mode enum (OAuth, Cedarling)
- **AuditPersistenceModeTest** - Tests for audit persistence mode enum (Internal, Config-API)
- **BaseDnConfigurationTest** - Tests for base DN configuration including audit DN

## Running Tests

Tests use TestNG framework with Mockito for mocking dependencies:

```bash
mvn test
```

## Test Patterns

All tests follow these patterns:
- Use descriptive test method names with format: `test<Method>_<Condition>_<ExpectedResult>`
- Mock external dependencies using Mockito
- Verify both happy paths and edge cases
- Test null handling and boundary conditions
- Use ArgumentCaptor for complex verification scenarios
