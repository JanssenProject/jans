/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */
package org.gluu.oxtrust.model.scim2;

//import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import static org.gluu.oxtrust.model.scim2.Constants.LIST_RESPONSE_SCHEMA_ID;

/**
 * This class models the contents of a search response. See section 3.4.2 RFC 7644.
 * @author Rahat Ali Date: 05.08.2015
 */
/*
 * Udpated by jgomer on 2017-10-01.
 */
public class ListResponse {

    private List<String> schemas;
    private int totalResults;
    private int startIndex;
    private int itemsPerPage;

    @JsonProperty("Resources")
    private List<BaseScimResource> resources;

    /**
     * Default no arg constructor. It creates a instance of <code>ListResponse</code> with the {@link #getSchemas() schemas}
     * properly initialized.
     */
    public ListResponse(){
        initSchemas();
    }

    /**
     * Constructs a list response with the arguments supplied, and {@link #getSchemas() schemas} initialized properly.
     * @param sindex Specifies a start index
     * @param ippage Specifies a number of items per page
     * @param total Specifies a total number of results
     */
    public ListResponse(int sindex, int ippage, int total){
        initSchemas();
        totalResults=total;
        startIndex=sindex;
        itemsPerPage=ippage;
        resources =new ArrayList<>();
    }

    private void initSchemas(){
        schemas=new ArrayList<>();
        schemas.add(LIST_RESPONSE_SCHEMA_ID);
    }

    /**
     * Adds the resource to the list of results of this <code>ListResponse</code>.
     * @param resource A SCIM resource
     */
    public void addResource(BaseScimResource resource){
        resources.add(resource);
    }

    public int getTotalResults() {
        return totalResults;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getItemsPerPage() {
        return itemsPerPage;
    }

    /**
     * Retrieves a list with all resources contained in this <code>ListResponse</code>.
     * @return A List of BaseScimResource objects
     */
    public List<BaseScimResource> getResources() {
        return resources;
    }

    public void setResources(List<BaseScimResource> resources) {
        this.resources = resources;
    }

    public List<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(List<String> schemas) {
        this.schemas = schemas;
    }

}
