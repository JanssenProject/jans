/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.util;

import io.grpc.Metadata;

/**
 * Utility for handling gRPC authorization headers
 *
 * @author Yuriy Movchan Date: 01/20/2026
 */
public class HeaderUtils {
    
    /**
     * Finds the authorization header in gRPC metadata, handling various
     * case variations and naming conventions.
     * 
     * @param headers The gRPC metadata headers
     * @return The authorization header value, or null if not found
     */
    public static String findAuthorizationHeader(Metadata headers) {
        if (headers == null) {
            return null;
        }
        
        // Common variations of authorization header keys
        String[] possibleKeys = {
            "authorization",
            "grpc-metadata-authorization",
            "x-authorization",
            "x-grpc-authorization"
        };
        
        for (String key : possibleKeys) {
            String value = headers.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        
        return null;
    }
    
    /**
     * Extracts the bearer token from an authorization header.
     * 
     * @param authHeader The full authorization header value
     * @return The token without the "bearer " prefix, or null if invalid
     */
    public static String extractBearerToken(String authHeader) {
        if (authHeader == null || authHeader.trim().isEmpty()) {
            return null;
        }
        
        // Remove any extra whitespace
        String trimmed = authHeader.trim();
        
        // Check if it's a bearer token
        if (trimmed.toLowerCase().startsWith("bearer ")) {
            return trimmed.substring(7).trim(); // Remove "bearer " prefix
        }
        
        // Also accept "Bearer " with capital B
        if (trimmed.startsWith("Bearer ")) {
            return trimmed.substring(7).trim();
        }
        
        // If it doesn't start with bearer, return as-is (might be basic auth or other)
        return trimmed;
    }
    
    /**
     * Combines both methods: finds and extracts the bearer token.
     * 
     * @param headers The gRPC metadata headers
     * @return The bearer token, or null if not found/invalid
     */
    public static String findAndExtractBearerToken(Metadata headers) {
        String authHeader = findAuthorizationHeader(headers);
        return extractBearerToken(authHeader);
    }

}