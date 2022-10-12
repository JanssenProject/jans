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

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.examples.Example;

import java.io.IOException;
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
/*
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;*/
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

@Provider
public class OASSchemaFileReader implements OASFilter {

    @Inject
    Logger log;

   
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    @Override

    public void filterOpenAPI(OpenAPI openAPI) {
        //log.debug("\n\n OASSchemaFileReader:filterOpenAPI() - openAPI:{}, params:{}, cookies:{}, headers:{}", openAPI, params, cookies, headers);
		log.debug("\n\n OASSchemaFileReader:filterOpenAPI() - openAPI:{}", openAPI);
        Components defaultComponents = OASFactory.createComponents();
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(defaultComponents);
        }

        try {
            //generateExamples().forEach(openAPI.getComponents()::addExample);
            generateExamples().entrySet().forEach(ex -> openAPI.getPaths().getPathItem("/api/generate").getPOST().getRequestBody().getContent().getMediaType(MediaType.APPLICATION_JSON).addExample(ex.getKey(), ex.getValue()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Map<String, Example> generateExamples() throws Exception {
        final Map<String, Example> examples = new LinkedHashMap<>();
        getFolderData(examples, "PLACE YOUR URL HERE");
        //getExamples(examples);
        return examples;
    }

    //If user has provided the folder then recursively loop over it to get the files and their contents
    private void getFolderData(final Map<String, Example> examples, final String inputURL) throws IOException {
        //Make the request to provided folder path and get the folder/files from it.
        final CloseableHttpResponse folderResponse = httpClient.execute(new HttpGet(inputURL));
        final String responseBody = EntityUtils.toString(folderResponse.getEntity(), StandardCharsets.UTF_8);

        //If the folder API request provides valid response and contains the list of files or folders then loop over it else its plain/text with direct contents
        if (folderResponse.getStatusLine().getStatusCode() == 200 && ContentType.get(folderResponse.getEntity()).toString().equalsIgnoreCase("application/json; charset=utf-8")) {

            final JSONArray jsonArray = new JSONArray(responseBody);

            jsonArray.forEach(item -> {
                final JSONObject obj = (JSONObject) item;

                if (obj.getString("type").equalsIgnoreCase("file")) {
                    //Make request to each file in the GitHub folder and obtain its contents
                    try {
                        final CloseableHttpResponse fileResponse = httpClient.execute(new HttpGet(obj.getString("download_url")));
                        //If the response code is 200 then add the contents to Example
                        if (fileResponse.getStatusLine().getStatusCode() == 200) {
                            final String fileResponseBody = EntityUtils.toString(fileResponse.getEntity(), StandardCharsets.UTF_8);
                            if (obj.getString("download_url").contains(".json")) {
                                examples.put(obj.getString("name"), OASFactory.createExample().value(objectMapper.readValue(fileResponseBody, ObjectNode.class)));
                            } else if (obj.getString("download_url").contains(".xml")) {
                                examples.put(obj.getString("name"), OASFactory.createExample().value(fileResponseBody));
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        getFolderData(examples, obj.getString("url"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

        } else if (folderResponse.getStatusLine().getStatusCode() == 200 && ContentType.get(folderResponse.getEntity()).toString().equalsIgnoreCase("text/plain; charset=utf-8")) {
            //if direct file provided then add its content
            examples.put(inputURL.substring(inputURL.lastIndexOf("/")), OASFactory.createExample().value(objectMapper.readValue(responseBody, ObjectNode.class)));
        }
    }

}
