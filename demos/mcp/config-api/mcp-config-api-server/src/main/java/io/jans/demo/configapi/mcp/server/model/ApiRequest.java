package io.jans.demo.configapi.mcp.server.model;

import java.util.Map;

public record ApiRequest(
        String action,
        Map<String, String> parameters) {
}
