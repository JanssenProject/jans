/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.multipleresource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim2.client.UserBaseTest;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeTest;

import jakarta.ws.rs.core.Response;
import java.time.*;
import java.util.*;

import static jakarta.ws.rs.core.Response.Status.*;

import static org.testng.Assert.*;

/**
 * Test devoted to /scim/UpdatedUsers endpoint
 */
public class UpdatedUsersTest extends UserBaseTest {
    
    private static final int MAX_USERS = 9;
    
    private int N;
    private List<String> inums;
    private ObjectMapper mapper;
    
    @BeforeTest
    public void init() {
        //Choose N in [1, MAX_USERS]
        N = 1 + randInt(MAX_USERS);
        
        inums = new ArrayList<>();
        mapper = new ObjectMapper();
    }
    
    @Test
    public void creatingUsers() throws Exception {
        
        String isoDate = null;
        //pick a rand number in [0, N-1]
        int i = randInt(N);
        
        //Create N random users
        logger.info("Creating {} users", N);
        for (int j = 0; j < N; j++) {
            UserResource user = getDummyPatient();
            Response response = client.createUser(user, "meta.created", null);
            
            assertEquals(response.getStatus(), CREATED.getStatusCode());
            user = response.readEntity(usrClass); 
            inums.add(user.getId());
            
            if (j == i) {
                isoDate = user.getMeta().getCreated();
//                logger.info("{}-indexed user created at '{}'", j, isoDate);
            }
        }
        
        Thread.sleep(1500);	//See #7
        
        logger.info("Querying created users after '{}'", isoDate);
        Response response = client.usersChangedAfter(isoDate, 0, N);
        assertEquals(response.getStatus(), OK.getStatusCode());
        
        String json = response.readEntity(String.class);
        //Convert response into an opaque map
        Map<String, Object> map = mapper.readValue(json, new TypeReference<Map<String, Object>>(){});
        
        //There should be N - i results
        assertEquals(map.get("total"), N - i);
        Set<String> foundInums = getFoundInums(map);
        
        for (int j = 0; j < inums.size(); j++) {
            assertEquals(j >= i, foundInums.contains(inums.get(j)));
        }
        
    }
    
    @Test(dependsOnMethods = "creatingUsers")
    public void updatingUsers() throws Exception {

        //pick 2 rand inums
        String A = inums.get(randInt(N));
        String B = inums.get(randInt(N));
        
        UserResource u = new UserResource();
        u.setActive(true);
        
        //Update active attribute for the chosen users
        Response response = client.updateUser(u, A, "meta.lastModified", null);
        assertEquals(response.getStatus(), OK.getStatusCode());
        
        String isoDate = response.readEntity(usrClass).getMeta().getLastModified();
        logger.info("User {} updated at '{}'", A, isoDate);
        
        response = client.updateUser(u, B, "id", null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        logger.info("Querying updated users after '{}'", isoDate);
        response = client.usersChangedAfter(isoDate, 0, N);
        assertEquals(response.getStatus(), OK.getStatusCode());

        String json = response.readEntity(String.class);
        //Convert response into an opaque map
        Map<String, Object> map = mapper.readValue(json, new TypeReference<Map<String, Object>>(){});        
        Set<String> foundInums = getFoundInums(map);

        assertTrue(foundInums.remove(A));
        assertTrue(A.equals(B) || foundInums.remove(B));
        
        //Ensure there are no false positives
        assertTrue(foundInums.isEmpty());
        
    }
 
    @Test(dependsOnMethods = "updatingUsers", alwaysRun = true)
    public void deleteUsers() {
        
        //Delete all users (assert is not used so the list can be thoroughly exhausted)
        for (String id : inums) {
            Response response = client.deleteUser(id);
            
            if (response.getStatus() == NO_CONTENT.getStatusCode()) {
                logger.info("User '{}' removed", id);
            } else {
                logger.error("Error removing user '{}'", id);
            }
        }
        
    }
    
    private UserResource getDummyPatient() {

        UserResource user = new UserResource();
        user.setUserName("test-" + Math.random());
        user.setDisplayName(user.getUserName());
        return user;
    }

    private Set<String> getFoundInums(Map<String, Object> map) throws Exception {
        
        Set<String> foundInums = new TreeSet<>();
        List<Map<String, Object>> results = mapper.convertValue(map.get("results"), 
            new TypeReference<List<Map<String, Object>>>(){});

        for (Map<String, Object> user : results) {
            Object inum = List.class.cast(user.get("inum")).get(0);
            foundInums.add(inum.toString());
        }
        
        return foundInums;
        
    }
    
    //Returns a random integer in [0, n - 1], n >= 1
    private int randInt(int n) {
        return Double.valueOf(Math.random() * n).intValue();
    }

}
