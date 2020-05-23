package org.gluu.oxauthconfigapi.rest;

import org.eclipse.microprofile.openapi.annotations.ExternalDocumentation;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/api/v1")
@OpenAPIDefinition(info = @Info(title = "OxAuth Configuration API.", description = "This API allows modifications of Gluu server oxauth configuration.", version = "1.0", contact = @Contact(name = "Gluu Inc.", url = "https://www.gluu.org/")), servers = {
		@Server(url = "http://localhost:8083") }, externalDocs = @ExternalDocumentation(url = "https://gluu.org/docs/", description = "Gluu documentation"), tags = {
				@Tag(name = "Oxauth api", description = "gluu oxauth configuration api."), })
public class ApiApplication extends Application {

}
