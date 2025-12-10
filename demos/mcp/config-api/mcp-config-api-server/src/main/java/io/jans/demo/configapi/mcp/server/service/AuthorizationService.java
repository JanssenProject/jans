package io.jans.demo.configapi.mcp.server.service;

import io.jans.cedarling.binding.wrapper.CedarlingAdapter;
import uniffi.cedarling_uniffi.*;
import io.jans.cedarling.binding.wrapper.CedarlingAdapter;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing user authorization and permissions
 */
public class AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);
    private static CedarlingAdapter cedarlingAdapter;

    public static AuthorizationService initAuthorizationService() {

        String bootstrapJsonStr = """
                    {
                    "CEDARLING_APPLICATION_NAME":   "MyApp",
                    "CEDARLING_POLICY_STORE_ID":    "your-policy-store-id",
                    "CEDARLING_USER_AUTHZ":         "enabled",
                    "CEDARLING_WORKLOAD_AUTHZ":     "enabled",
                    "CEDARLING_LOG_LEVEL":          "INFO",
                    "CEDARLING_LOG_TYPE":           "std_out",
                    "CEDARLING_POLICY_STORE_LOCAL_FN": "/path/to/policy-store.json"
                }
                """;

        try {
            cedarlingAdapter = new CedarlingAdapter();
            cedarlingAdapter.loadFromJson(bootstrapJsonStr);
        } catch (CedarlingException e) {
            System.out.println("Unable to initialize Cedarling" + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unable to initialize Cedarling" + e.getMessage());
        }

        AuthorizationService authorizationService = new AuthorizationService();
        return authorizationService;
    }

    /**
     * Check if current user has a specific permission
     */
    public boolean checkAuthorization(Map<String, String> tokens, String action, String resource, String context)
            throws UnauthorizedException {

        // Perform authorization
        try {
            AuthorizeResult result = cedarlingAdapter.authorize(tokens, action, new JSONObject(resource),
                    new JSONObject(context));
            if (result.getDecision()) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        } catch (AuthorizeException e) {
            logger.error("Unable to authorize user", e);
            // TODO: don't swallow these exceptions
        } catch (EntityException e) {
            logger.error("Unable to authorize user", e);
            // TODO: don't swallow these exceptions
        }
        return Boolean.FALSE;
    }

    /**
     * Exception thrown when authorization fails
     */
    public static class UnauthorizedException extends Exception {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
}
