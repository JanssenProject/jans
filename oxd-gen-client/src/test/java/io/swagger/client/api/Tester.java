package io.swagger.client.api;

import io.swagger.client.ApiClient;

import java.util.List;

import static org.testng.Assert.assertTrue;


/**
 * @author yuriyz
 */
public class Tester {

    private Tester() {
    }

    public static DevelopersApi api() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath("http://localhost:8084");
        apiClient.setVerifyingSsl(false);
        apiClient.setDebugging(true);

        return new DevelopersApi(apiClient);
    }

    public static void notEmpty(String str) {
        assertTrue(str != null && !str.isEmpty());
    }

    public static void notEmpty(List<String> str) {
        assertTrue(str != null && !str.isEmpty() && str.get(0) != null && !str.get(0).isEmpty());
    }
}
