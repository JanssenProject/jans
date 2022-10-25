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

import io.swagger.v3.oas.models.media.MediaType;
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
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.core.filter.AbstractSpecFilter;
import io.swagger.v3.core.model.ApiDescription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

import java.io.*;
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

    private static final String RESOURCE_FOLDER = "";

    @Override
    public Optional<OpenAPI> filterOpenAPI(OpenAPI openAPI, Map<String, List<String>> params,
            Map<String, String> cookies, Map<String, List<String>> headers) {
        try {
            System.out.println("\n\n\n *********** SpecFilter::filterOpenAPI() *********** \n\n\n");
            System.out.println("\n SpecFilter::filterOpenAPI() - openAPI=" + openAPI + " , params=" + params
                    + " , cookies=" + cookies + " , headers=" + headers + "\n");
            if (openAPI != null) {
                System.out.println("\n SpecFilter::filterOpenAPI() - openAPI.getPaths()= " + openAPI.getPaths()
                        + " openAPI..getServers()=" + openAPI.getServers());
                if (openAPI.getPaths() != null) {
                    Paths paths = openAPI.getPaths();
                    if (paths != null && !paths.isEmpty()) {
                        Set<Map.Entry<String, PathItem>> pathEntry = paths.entrySet();
                        for (Map.Entry<String, PathItem> entry : pathEntry) {

                            System.out.println("\n SpecFilter::filterOpenAPI() - entry.getKey()()= " + entry.getKey()
                                    + " entry.getValue()=" + entry.getValue() + " , entry.getValue().readOperations()="
                                    + entry.getValue().readOperations());
                            //InputStream is = getFileAsIOStream("attribute.json");
                           // printFileContent(is);

                            InputStream is = getFileAsIOStream("example/attribute.json");
                            printFileContent(is);

                        }
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("*********** SpecFilter::filterOpenAPI() - ex = " + ex);
        }
        return Optional.of(openAPI);
    }

    @Override
    public Optional<Operation> filterOperation(Operation operation, ApiDescription api,
            Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        System.out.println("*********** SpecFilter::filterOperation() *********** \n\n\n");
        try {

            System.out.println("\n SpecFilter::filterOperation() - operation=" + operation + "api=" + api + " , params="
                    + params + " , cookies=" + cookies + " , headers=" + headers + "\n");
            if (operation != null) {
                System.out.println("\n SpecFilter::filterOperation() - operation.getOperationId() = "
                        + operation.getOperationId() + " , operation.getRequestBody()= " + operation.getRequestBody());
               
                if (operation.getRequestBody() != null) {
                    System.out.println("\n SpecFilter::filterOperation() - operation.getRequestBody().getContent() = "+operation.getRequestBody().getContent());
                    if(operation.getRequestBody().getContent()!=null) {
                    for (MediaType mediaType : operation.getRequestBody().getContent().values()) {
                        if (mediaType == null || mediaType.getExamples() ==null ||mediaType.getExamples().values() ==null)
                            continue;
                        for (Example example : mediaType.getExamples().values()) {
                            if(example==null) {
                                continue;
                            }
                            System.out.println("\n SpecFilter::filterOperation() - example.getExternalValue() ="+example.getExternalValue());
                            example.getExternalValue();
                            // external value contains a path to local resource
                            // get that resource and set it to values as JSON
                            example.setValue("{\"someExampleParam\": 1}");
                        }
                    }
                    }

                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("*********** SpecFilter::filterOpenAPI() - ex = " + ex);
        }
        return Optional.of(operation);
    }

    @Override
    public Optional<PathItem> filterPathItem(PathItem pathItem, ApiDescription api, Map<String, List<String>> params,
            Map<String, String> cookies, Map<String, List<String>> headers) {
        System.out.println("\n\n\n *********** SpecFilter::filterPathItem() *********** ");

        System.out.println("SpecFilter::filterPathItem() *********** \n\n\n");
        return Optional.of(pathItem);
    }

    private InputStream getFileAsIOStream(final String fileName) {
        System.out.println("SpecFilter::filterPathItem() fileName = " + fileName);
        InputStream ioStream = this.getClass().getClassLoader().getResourceAsStream(fileName);

        
        if (ioStream == null) {
            //throw new IllegalArgumentException(fileName + " is not found");
            System.out.println("SpecFilter::filterPathItem() "+fileName+" is not found");
        }

        System.out.println("SpecFilter::filterPathItem() ioStream = " + ioStream);
        return ioStream;
    }

    private void printFileContent(InputStream is) throws IOException {
        if(is==null) {
            return;
        }
        try (InputStreamReader isr = new InputStreamReader(is); BufferedReader br = new BufferedReader(isr);) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            is.close();
        }
    }
}