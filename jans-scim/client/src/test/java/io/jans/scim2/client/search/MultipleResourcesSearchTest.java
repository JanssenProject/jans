/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.search;

import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim2.client.BaseTest;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.OK;
import static org.testng.Assert.*;

/**
 * Created by jgomer on 2017-10-26.
 */
public class MultipleResourcesSearchTest extends BaseTest {

    private SearchRequest sr;
    private ListResponse listResponse;

    @Parameters({"search_multiple_1","search_multiple_2"})
    @Test
    public void searchJson(String json1, String json2){

        Response response=client.searchResourcesPost(json1);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse anotherLR=response.readEntity(ListResponse.class);
        //Verify it has results
        assertTrue(anotherLR.getTotalResults()>0);

        response=client.searchResourcesPost(json2);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse anotherLR2 =response.readEntity(ListResponse.class);

        assertEquals(anotherLR.getTotalResults(), anotherLR2.getTotalResults());
        assertNull(anotherLR2.getResources());
        assertEquals(anotherLR2.getItemsPerPage(), 0);   //unassigned
        assertEquals(anotherLR2.getStartIndex(), 0); //unassigned

    }

    @Test(dependsOnMethods = "searchJson")
    public void search1(){

        //Build up a request
        sr = new SearchRequest();
        sr.setAttributes("id"); //return a few attributes
        sr.setStartIndex(1);
        sr.setFilter("displayName co \"1111\" or displayName co \"Group\"");    //Aimed at having both users and groups

        Response response = client.searchResourcesPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        listResponse=response.readEntity(ListResponse.class);

        //Verify it has results
        assertTrue(listResponse.getTotalResults()>0);

    }

    @Test(dependsOnMethods = "search1")
    public void search2(){

        //Move forward the start index
        sr.setStartIndex(2);

        Response response = client.searchResourcesPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse anotherLR=response.readEntity(ListResponse.class);

        //Verify one result less was returned
        assertEquals(listResponse.getTotalResults(), anotherLR.getResources().size()+1);
        assertEquals(anotherLR.getResources().size(), anotherLR.getItemsPerPage());

    }

    @Test(dependsOnMethods = "search2")
    public void search3(){

        //Move the start index to the last item
        sr.setStartIndex(listResponse.getTotalResults());

        Response response = client.searchResourcesPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse anotherLR=response.readEntity(ListResponse.class);

        //Verify there is only one result
        assertEquals(anotherLR.getItemsPerPage(), 1);

    }

    @Test(dependsOnMethods = "search3")
    public void search4(){

        //Verify there are no results when start index is greater than the number of available results
        sr.setStartIndex(listResponse.getTotalResults()+1);

        Response response = client.searchResourcesPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse anotherLR=response.readEntity(ListResponse.class);

        assertEquals(anotherLR.getItemsPerPage(), 0);   //unassigned
        assertEquals(anotherLR.getStartIndex(), 0);   //unassigned

    }

    @Test(dependsOnMethods = "search4")
    public void search5(){

        sr.setStartIndex(null); //Means 1
        sr.setCount(0);     //Returns no resources, only total

        Response response = client.searchResourcesPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse anotherLR=response.readEntity(ListResponse.class);

        assertEquals(listResponse.getTotalResults(), anotherLR.getTotalResults());
        assertNull(anotherLR.getResources());
        assertEquals(anotherLR.getItemsPerPage(), 0);   //unassigned
        assertEquals(anotherLR.getStartIndex(), 0); //unassigned

    }

    @Test(dependsOnMethods = "search5")
    public void search6(){

        sr.setStartIndex(5);    //Irrelevant since no results will be returned
        sr.setCount(0);     //Returns no resources, only total

        Response response = client.searchResourcesPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse anotherLR=response.readEntity(ListResponse.class);

        assertEquals(listResponse.getTotalResults(), anotherLR.getTotalResults());
        assertNull(anotherLR.getResources());
        assertEquals(anotherLR.getItemsPerPage(), 0);   //unassigned
        assertEquals(anotherLR.getStartIndex(), 0); //unassigned

    }

}
