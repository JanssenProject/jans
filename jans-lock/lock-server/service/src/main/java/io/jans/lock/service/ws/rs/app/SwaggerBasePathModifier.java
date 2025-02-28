package io.jans.lock.service.ws.rs.app;

import io.jans.lock.util.Constants;
import io.swagger.v3.jaxrs2.ReaderListener;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.integration.api.OpenApiReader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;

@OpenAPIDefinition 
public class SwaggerBasePathModifier implements ReaderListener {

	@Override
	public void beforeScan(OpenApiReader reader, OpenAPI openAPI) {
	}

	@Override
	public void afterScan(OpenApiReader reader, OpenAPI openAPI) {
		Paths paths = openAPI.getPaths();
		Paths modifiedPaths = new Paths();

		paths.forEach((path, pathItem) -> {
			if (path.startsWith("/.well-known")) {
				modifiedPaths.addPathItem(path, pathItem);
			} else {
				modifiedPaths.addPathItem(Constants.BASE_PATH + path, pathItem);
			}
		});

		openAPI.setPaths(modifiedPaths);
	}
}
