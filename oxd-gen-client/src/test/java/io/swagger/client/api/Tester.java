package io.swagger.client.api;

import io.swagger.client.ApiClient;
import io.swagger.client.Configuration;

import java.util.List;

import static org.testng.Assert.assertTrue;


/**
 * @author yuriyz
 */
public class Tester {

    static {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath("http://localhost:8084");
        apiClient.setVerifyingSsl(false);
        apiClient.setDebugging(true);

        Configuration.setDefaultApiClient(apiClient);
    }

    public static void notEmpty(String str) {
        assertTrue(str != null && !str.isEmpty());
    }

    public static void notEmpty(List<String> str) {
        assertTrue(str != null && !str.isEmpty() && str.get(0) != null && !str.get(0).isEmpty());
    }
}
