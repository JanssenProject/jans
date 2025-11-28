package io.jans.demo.configapi.mcp.server.model;

import java.util.Map;

public record ApiResponse(
        String id,
        String name,
        String description,
        Map<String, Object> metadata) {
}
