# FIDO2 Device Registration Integration Test

This directory contains integration tests for FIDO2 device registration functionality.

## Test Files

- `Fido2DeviceRegistrationBasicTest.java` - Basic test for FIDO2 device registration
- `README.md` - This documentation file

## Running the Test

### Prerequisites

1. Ensure Jans FIDO2 server is properly configured
2. Test data should be loaded in the database
3. Maven and Java should be available

### Running the Test

#### Option 1: Using Maven directly
```bash
cd jans-fido2/server
mvn -DsuiteXmlFile=src/test/resources/testng.xml clean test
```

#### Option 2: Run specific test class
```bash
cd jans-fido2/server
mvn -Dtest=Fido2DeviceRegistrationBasicTest clean test
```

## Test Coverage

The test covers:

1. **Service Availability** - Verifies FIDO2 service is running and configured
2. **Configuration Validation** - Checks FIDO2 configuration is properly loaded
3. **Attestation Options** - Tests ability to generate device registration options
4. **Test Data** - Validates test user and device data exists

## Expected Results

If the test passes, you should see:
```
✅ FIDO2 Device Registration Test PASSED!
   - FIDO2 service is available and configured
   - Attestation options can be generated
   - Test data is properly loaded
   - Configuration validation passed
```

## Troubleshooting

### Common Issues

1. **TestNG dependency missing** - Ensure TestNG is in pom.xml
2. **Configuration not loaded** - Check FIDO2 service configuration
3. **Test data not found** - Verify test data is loaded in database
4. **Service not available** - Ensure FIDO2 service is running

## Build Integration

This test is designed to run during the build process and will:
- Automatically configure the test environment
- Load test data if not present
- Validate FIDO2 device registration functionality
- Report results as part of the build 