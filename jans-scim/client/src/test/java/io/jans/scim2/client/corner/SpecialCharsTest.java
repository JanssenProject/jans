package io.jans.scim2.client.corner;

import org.apache.commons.lang3.StringEscapeUtils;

import io.jans.orm.util.StringHelper;
import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim2.client.UserBaseTest;
import io.jans.scim2.listener.SkipTest;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jakarta.ws.rs.core.Response.Status.OK;
import static org.testng.Assert.*;

@SkipTest(databases = { "couchbase" })
public class SpecialCharsTest extends UserBaseTest {

    private static final String[] SPECIAL_CHARS = new String[]{"*", "\\", "(", ")"};    //, "\0" (see nullChar test)
    private List<String> specialFilterLdapChars = null;

    private List<String> userNames;

    @BeforeTest
    private void addOne() {
        specialFilterLdapChars = Stream.of(SPECIAL_CHARS).map(StringHelper::escapeJson).collect(Collectors.toList());
        //Per customer request
        specialFilterLdapChars.add(StringHelper.escapeJson("/"));
    }

    @Test
    public void containabilityAny() {

        //Builds a long "or" based clause
        String filter = specialFilterLdapChars.stream().reduce("", (partial, next) -> partial + String.format(" or userName co \"%s\"", next));
        SearchRequest sr = new SearchRequest();
        sr.setFilter(filter.substring(4));   //Drop beginning (namely ' or ')
        sr.setAttributes("userName");

        //Search users whose usernames contain ANY of the chars
        Response response = client.searchUsersPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        List<UserResource> resources = response.readEntity(ListResponse.class).getResources().stream()
                .map(UserResource.class::cast).collect(Collectors.toList());

        assertTrue(resources.size() > 0);
        resources.forEach(user -> assertTrue(specialFilterLdapChars.stream().anyMatch(ch -> user.getUserName().contains(ch))));

        userNames = resources.stream().map(UserResource::getUserName).collect(Collectors.toList());

    }

    @SkipTest(databases = { "spanner", "couchbase" })
    @Test
    public void containabilityAll() {

        //Builds a long "and" based clause
        String filter = specialFilterLdapChars.stream().reduce("", (partial, next) -> partial + String.format(" and userName co \"%s\"", next));
        SearchRequest sr = new SearchRequest();
        sr.setFilter(filter.substring(4));   //Drop beginning (namely " and ")
        sr.setAttributes("userName");

        //Search users whose usernames contain ALL the chars
        Response response = client.searchUsersPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        List<UserResource> resources = response.readEntity(ListResponse.class).getResources().stream()
                .map(UserResource.class::cast).collect(Collectors.toList());

        String userName = resources.get(0).getUserName();
        assertEquals(resources.size(), 1);
        assertTrue(Stream.of(SPECIAL_CHARS).allMatch(userName::contains));

    }

    @SkipTest(databases = { "spanner", "couchbase" })
    @Test
    public void containabilityAllInGivenName() {

        String filter = specialFilterLdapChars.stream().reduce("", (partial, next) -> partial + String.format(" and name.givenName co \"%s\"", next));
        SearchRequest sr = new SearchRequest();
        sr.setFilter(filter.substring(5));   //Drop beginning (namely ' and ')
        sr.setAttributes("name");

        //Search users whose given names contain ALL the chars
        Response response = client.searchUsersPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        List<UserResource> resources = response.readEntity(ListResponse.class).getResources().stream()
                .map(UserResource.class::cast).collect(Collectors.toList());

        String givenName = resources.get(0).getName().getGivenName();
        assertEquals(resources.size(), 1);
        assertTrue(Stream.of(SPECIAL_CHARS).allMatch(givenName::contains));

    }

    @Test(dependsOnMethods = "containabilityAny")
    public void equality() {

        //Based on usernames list built in test containabilityAny, n searches with EQ filter are issued
        for (String userName : userNames) {

            SearchRequest sr = new SearchRequest();
            sr.setFilter(String.format("userName eq \"%s\"", StringEscapeUtils.escapeJson(userName)));
            sr.setAttributes("userName, name.givenName");

            Response response = client.searchUsersPost(sr);
            UserResource user = (UserResource) response.readEntity(ListResponse.class).getResources().get(0);
            logger.info("User {} found with userName {}", user.getName().getGivenName(), user.getUserName());
            assertEquals(userName, user.getUserName());

        }

    }

    //This test has been disabled as dealing with the NUL char in text editors and terminal consoles turned out to be troublesome
    //However, in practice, translation of \0 into \00 is taking place in server side
    //@Test
    public void NullChar() {

        SearchRequest sr = new SearchRequest();
        sr.setFilter(String.format("displayName co \"%s\"", "\0"));
        sr.setAttributes("userName");

        Response response = client.searchUsersPost(sr);
        assertEquals(response.readEntity(ListResponse.class).getResources().size(), 1);

    }

}
