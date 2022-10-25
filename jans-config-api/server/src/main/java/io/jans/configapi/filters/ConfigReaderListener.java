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
import io.swagger.v3.oas.annotations.Operation;
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
import io.swagger.v3.oas.annotations.tags.*;

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

//@SwaggerDefinition
public class ConfigReaderListener implements ReaderListener {

    @Inject
    Logger log;
   
    @Override
    public void beforeScan(OpenApiReader openApiReader, OpenAPI openAPI) {
        log.error("ConfigReaderListener::beforeScan() - openApiReader:{}, openAPI:{} ", openApiReader, openAPI);
        log.error("ConfigReaderListener::beforeScan() - openAPI.getComponents():{} ", openAPI.getComponents());
        log.error("ConfigReaderListener::beforeScan() - openAPI.getPaths():{} ", openAPI.getPaths());
        System.out.println("\n\n\n *********** ConfigReaderListener::beforeScan() *********** \n\n\n");
        if(openAPI.getPaths()!=null && openAPI.getPaths().size()>0) {
            
          Map<String,PathItem> map = openAPI.getPaths().entrySet().stream()
                  .filter(k -> (k.getValue() == null))
                  .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
          log.error("ConfigReaderListener::beforeScan() - map:{}", map);
          if(map==null || map.isEmpty()) {
              log.error("ConfigReaderListener::beforeScan() - map is null");              
          }
            
        }
    }

    @Override
    public void afterScan(OpenApiReader openApiReader, OpenAPI openAPI) {
        log.error("ConfigReaderListener::afterScan() - openApiReader:{}, openAPI:{} ", openApiReader, openAPI);
        log.error("ConfigReaderListener::afterScan() - openAPI.getComponents():{} ", openAPI.getComponents());
        log.error("ConfigReaderListener::afterScan() - openAPI.getPaths():{} ", openAPI.getPaths());
        System.out.println("\n\n\n *********** ConfigReaderListener::afterScan() *********** \n\n\n");
        PathItem expectedPath = new PathItem().$ref("http://my.company.com/paths/health.json");
        openAPI.path("/test-puja", expectedPath);
        openAPI.addTagsItem( new io.swagger.v3.oas.models.tags.Tag().description("ABC-Tag"));
        openAPI.addExtension("name", "krishna");
        
        if(openAPI.getPaths()!=null && openAPI.getPaths().size()>0) {
            
          Map<String,PathItem> map = openAPI.getPaths().entrySet().stream()
                  .filter(k -> (k.getValue() == null))
                  .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
          log.error("ConfigReaderListener::afterScan() - map:{}", map);
          if(map==null || map.isEmpty()) {
              log.error("ConfigReaderListener::afterScan() - map is null");              
          }
            
        }
    }
/*
    Map<String, Example> generateExamples() throws Exception {
        final Map<String, Example> examples = new LinkedHashMap<>();
        /getFolderData(examples, "PLACE YOUR URL HERE");
        //getExamples(examples);
        return examples;
    }*/

    //If user has provided the folder then recursively loop over it to get the files and their contents
 /*   private void getFolderData(final Map<String, Example> examples, final String inputURL) throws IOException {
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
    }*/

}
