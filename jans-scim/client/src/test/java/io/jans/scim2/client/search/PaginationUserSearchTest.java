package io.jans.scim2.client.search;

import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim2.client.UserBaseTest;

import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;

import java.util.LinkedHashSet;
import java.util.Set;

import static jakarta.ws.rs.core.Response.Status.OK;

import static org.testng.Assert.*;

public class PaginationUserSearchTest extends UserBaseTest {

    private ListResponse listResponse;
    private SearchRequest sr;

    @Test
    public void search1() {

        //Issue a default request except for a filter and reducing the attributes returned
        sr = new SearchRequest();
        sr.setFilter("name.familyName co \"Filter\"");
        sr.setSortBy("id");    //Couchbase result set order is not deterministic, requires ORDER BY to workaround it
        sr.setAttributes("meta.location");

        Response response = client.searchUsersPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        //Store for further comparison
        listResponse = response.readEntity(ListResponse.class);
        assertEquals(listResponse.getResources().size(), listResponse.getTotalResults());

    }

    @Test(dependsOnMethods = "search1")
    public void search2() {

        //Issue the request specifiying index and count
        sr.setStartIndex(1);
        sr.setCount(listResponse.getTotalResults());

        Response response = client.searchUsersPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse anotherLR = response.readEntity(ListResponse.class);

        //number of results should be the same as in original listResponse (first test)
        assertEquals(listResponse.getTotalResults(), anotherLR.getTotalResults());
        //The page return should contain all results
        assertEquals(listResponse.getTotalResults(), anotherLR.getItemsPerPage());
        assertEquals(anotherLR.getStartIndex(), 1);

    }

    @Test(dependsOnMethods = "search2")
    public void search3() {

        int rounds=3;
        int ipp = listResponse.getTotalResults()/3;
        //Make several requests changing start index, leaving count to be fixed valued
        for (int i = 0; i < rounds; i++) {
            int startIndex = rounds*(1 + i) + 1;
            sr.setStartIndex(startIndex);
            sr.setCount(ipp);

            Response response = client.searchUsersPost(sr);
            assertEquals(response.getStatus(), OK.getStatusCode());

            ListResponse anotherLR = response.readEntity(ListResponse.class);

            //Items per page should be the same as count supplied above
            assertEquals(anotherLR.getItemsPerPage(), ipp);
            assertEquals(anotherLR.getStartIndex(), startIndex);

            //Determine if current results contain the same elements as in original listResponse
            Set<String> ids = new LinkedHashSet<>();
            Set<String> ids2 = new LinkedHashSet<>();
            for (int j = 0; j < ipp; j++) {
                ids.add(listResponse.getResources().get(j + startIndex - 1).getId());
                ids2.add(anotherLR.getResources().get(j).getId());
                assertNotNull(anotherLR.getResources().get(j).getMeta().getLocation());
            }

            assertTrue(ids.containsAll(ids2));
        }

    }

    @Test(dependsOnMethods = "search3")
    public void search4() {

        //Issue a request disregarding resources (only total is of interest)
        sr.setStartIndex(null);
        sr.setCount(0);

        Response response = client.searchUsersPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse anotherLR = response.readEntity(ListResponse.class);
        assertEquals(anotherLR.getItemsPerPage(), 0);    //means no existing
        assertEquals(anotherLR.getTotalResults(), listResponse.getTotalResults());
        //Verify resources weren't serialized
        assertNull(anotherLR.getResources());

    }

    @Test(dependsOnMethods = "search4")
    public void search5(){

        //Request more resources than existing
        int existing=listResponse.getTotalResults();
        sr.setStartIndex(null);
        sr.setCount(1+existing);

        Response response = client.searchUsersPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse anotherLR = response.readEntity(ListResponse.class);
        assertEquals(anotherLR.getTotalResults(), existing);
        assertEquals(anotherLR.getItemsPerPage(), existing);
        assertEquals(anotherLR.getStartIndex(), 1);

    }

}
