package io.jans.demo.configapi.mcp.server.service;

import io.jans.cedarling.binding.wrapper.CedarlingAdapter;
import uniffi.cedarling_uniffi.*;
import io.jans.cedarling.binding.wrapper.CedarlingAdapter;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.List;
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
                        "CEDARLING_APPLICATION_NAME": "My App",
                        "CEDARLING_ID_TOKEN_TRUST_MODE": "never",
                        "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED": [
                            "HS256",
                            "RS256"
                        ],
                        "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
                        "CEDARLING_LOG_LEVEL": "INFO",
                        "CEDARLING_LOG_TYPE": "std_out",
                        "CEDARLING_MAPPING_USER": "Jans::User",
                        "CEDARLING_MAPPING_WORKLOAD": "Jans::Workload",
                        "CEDARLING_POLICY_STORE_URI": "https://raw.githubusercontent.com/JanssenProject/CedarlingQuickstart/refs/heads/main/6d9f73b2d44ad4e7aa8f1182cde9f72dcbaa244f4327.json",
                        "CEDARLING_PRINCIPAL_BOOLEAN_OPERATION": {
                            "===": [
                            {
                                "var": "Jans::User"
                            },
                            "ALLOW"
                            ]
                        },
                        "CEDARLING_USER_AUTHZ": "enabled",
                        "CEDARLING_WORKLOAD_AUTHZ": "disabled",
                        "id": "97ef28fb-58a4-4a1c-9b05-e948e2c3be4f"
                    }
                """;

        try {
            System.out.println("===========Initializing Cedarling===========");
            cedarlingAdapter = new CedarlingAdapter();
            System.out.println("===========Adapter initialized successfully===========");
            cedarlingAdapter.loadFromJson(bootstrapJsonStr);
            System.out.println("===========Cedarling loaded successfully===========");
            Cedarling cedarling = cedarlingAdapter.getCedarling();
            System.out.println("===========Cedarling created successfully===========");
            if (cedarling == null) {
                System.out.println("===========Cedarling initialized failed===========");
                throw new Exception("Unable to initialize Cedarling");
            } else {
                System.out.println("===========Cedarling initialized successfully===========");
            }
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
     * Check if current user has a specific permission using unsigned authz
     */
    public boolean checkAuthorizationUnsigned(String principals, String action, String resource, String context)
            throws UnauthorizedException, EntityException {

        List<EntityData> principalsList = List.of(EntityData.Companion.fromJson(principals));
        // Perform authorization
        try {
            AuthorizeResult result = cedarlingAdapter.authorizeUnsigned(principalsList, action,
                    new JSONObject(resource),
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
