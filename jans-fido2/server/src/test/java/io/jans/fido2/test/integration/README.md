# FIDO2 Device Registration Integration Tests

This directory contains comprehensive integration tests for FIDO2 device registration functionality.

## Test Files

- `Fido2DeviceRegistrationBasicTest.java` - Basic FIDO2 service validation and attestation options generation
- `Fido2DeviceRegistrationEnhancedTest.java` - Enhanced comprehensive FIDO2 testing including assertion, performance, and security validation
- `README.md` - This documentation file

## Running the Tests

### Prerequisites

1. Ensure Jans FIDO2 server is properly configured
2. Test data should be loaded in the database
3. Maven and Java should be available

### Running Basic Tests

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

### Running Enhanced Tests

#### Option 1: Run enhanced test class
```bash
cd jans-fido2/server
mvn -Dtest=Fido2DeviceRegistrationEnhancedTest clean test
```

#### Option 2: Run all FIDO2 tests
```bash
cd jans-fido2/server
mvn -DsuiteXmlFile=src/test/resources/testng.xml clean test
```



## Test Coverage

### Basic Test Coverage
1. **Service Availability** - Verifies FIDO2 service is running and configured
2. **Configuration Validation** - Checks FIDO2 configuration is properly loaded
3. **Attestation Options** - Tests ability to generate device registration options
4. **Test Data** - Validates test user and device data exists

### Enhanced Test Coverage
1. **Complete Attestation Flow** - Comprehensive attestation options generation and validation
2. **Assertion Flow Testing** - Tests assertion options generation for authentication
3. **Error Handling** - Validates proper error handling for invalid requests
4. **Comprehensive Configuration** - Validates all FIDO2 configuration settings
5. **Test Data Integrity** - Comprehensive validation of test data
6. **Performance Testing** - Tests service performance and response times
7. **Algorithm Support** - Tests supported FIDO2 algorithms (ES256, RS256, etc.)
8. **Security Settings** - Validates security configurations and settings

## Enhanced Test Features

### Advanced Testing Capabilities
- **Data Provider Tests** - Parameterized testing for different algorithms
- **Performance Validation** - Response time testing for multiple operations
- **Security Validation** - Comprehensive security settings verification
- **Error Scenario Testing** - Invalid request handling validation
- **Comprehensive Logging** - Detailed logging for debugging and monitoring

### Test Methods in Enhanced Test
1. `testEnhancedAttestationOptionsGeneration()` - Complete attestation flow validation
2. `testAssertionOptionsGeneration()` - Assertion options generation testing
3. `testAttestationErrorHandling()` - Error handling validation
4. `testComprehensiveConfigurationValidation()` - Full configuration validation
5. `testTestDataIntegrity()` - Test data integrity verification
6. `testServicePerformance()` - Performance and response time testing
7. `testSupportedAlgorithms()` - Algorithm support validation (parameterized)
8. `testSecuritySettingsValidation()` - Security settings verification

## Expected Results

### Basic Test Results
If the basic test passes, you should see:
```
✅ FIDO2 Device Registration Basic Test PASSED!
   - FIDO2 service is available and configured
   - Attestation options can be generated
   - Test data is properly loaded
   - Configuration validation passed
```

### Enhanced Test Results
If the enhanced test passes, you should see:
```
✅ FIDO2 Device Registration Enhanced Test PASSED!
   - Complete attestation flow validated
   - Assertion options generation working
   - Error handling properly implemented
   - Comprehensive configuration validated
   - Test data integrity verified
   - Performance benchmarks met
   - Algorithm support validated
   - Security settings verified
```

## Troubleshooting

### Common Issues

1. **TestNG dependency missing** - Ensure TestNG is in pom.xml
2. **Configuration not loaded** - Check FIDO2 service configuration
3. **Test data not found** - Verify test data is loaded in database
4. **Service not available** - Ensure FIDO2 service is running
5. **Performance failures** - Check server resources and configuration
6. **Security validation failures** - Verify security settings in configuration

### Debug Mode
Run tests with debug logging:
```bash
mvn test -Dtest=*Fido2DeviceRegistration*Test -Dlogging.level.io.jans.fido2=DEBUG
```

### Performance Testing
Monitor test performance:
```bash
mvn test -Dtest=Fido2DeviceRegistrationEnhancedTest#testServicePerformance
```

## Build Integration

This test is designed to run during the build process and will:
- Automatically configure the test environment
- Load test data if not present
- Validate FIDO2 device registration functionality
- Perform comprehensive testing including performance and security
- Report results as part of the build

## Quality Gates

### Test Quality Requirements
- Code coverage should be maintained
- No critical security issues
- Performance benchmarks met
- All configuration validations pass
- Error handling properly implemented
- Algorithm support validated 