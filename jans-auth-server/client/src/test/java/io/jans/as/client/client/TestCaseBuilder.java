package io.jans.as.client.client;

import io.jans.as.client.client.testcasebuilders.RegistrationTestCase;

public class TestCaseBuilder {

    public static RegistrationTestCase registrationTestCaseBuilder(String title) {
        return new RegistrationTestCase(title);
    }

}
