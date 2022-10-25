/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.jaxrs2.ReaderListener;
import io.swagger.v3.oas.integration.api.OpenApiReader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.core.filter.AbstractSpecFilter;
import io.swagger.v3.core.model.ApiDescription;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

public class SpecFilter extends AbstractSpecFilter {

    @Override
    public Optional<OpenAPI> filterOpenAPI(OpenAPI openAPI, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        
        System.out.println("\n\n\n *********** SpecFilter::filterOpenAPI() *********** \n\n\n");
        System.out.println("\n openAPI="+openAPI+" , params="+params+" , cookies="+cookies+" , headers="+headers+"\n");
        if(openAPI!=null) {
            System.out.println("\n SpecFilter::filterOpenAPI() - openAPI.getPaths()= "+openAPI.getPaths()+" openAPI..getServers()="+openAPI.getServers());
            if(openAPI.getPaths()!=null) {
                Paths paths = openAPI.getPaths();
                for(Path)
            }
        }
        return Optional.of(openAPI);
      }
    
    @Override
    public Optional<Operation> filterOperation(Operation operation, ApiDescription api, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        System.out.println("\n\n\n *********** SpecFilter::filterOperation() *********** \n\n\n");
        return Optional.of(operation);
      }
    
    
    @Override
    public Optional<PathItem> filterPathItem(PathItem pathItem, ApiDescription api, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        System.out.println("\n\n\n *********** SpecFilter::filterOperation() *********** ");
        
        System.out.println("SpecFilter::filterOperation() *********** \n\n\n");
      return Optional.of(pathItem);
    }
}