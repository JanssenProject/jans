package gluu.scim2.client.corner;

import gluu.scim2.client.UserBaseTest;
import org.gluu.oxtrust.model.scim2.user.UserResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.*;

import static org.testng.Assert.*;
/**
 * Created by jgomer on 2017-11-01.
 */
public class WrongPayloadUserTest extends UserBaseTest {

    private UserResource user;

    @Parameters({"wrong_user_create_1"})
    @Test(groups = "A")
    public void createWrongAttrs1(String json){
        Response response=client.createUser(json, null, null);
        assertNotEquals(response.getStatus(), CREATED.getStatusCode());
    }

    @Parameters({"wrong_user_create_2"})
    @Test(groups = "A")
    public void createWrongAttrs2(String json){
        Response response=client.createUser(json, null, null);
        assertNotEquals(response.getStatus(), CREATED.getStatusCode());
    }

    @Parameters({"wrong_user_create_3"})
    @Test(groups = "A")
    public void createWrongAttrs3(String json){
        Response response=client.createUser(json, null, null);
        assertNotEquals(response.getStatus(), CREATED.getStatusCode());
    }

    @Parameters("user_minimal_create")
    @Test(dependsOnGroups = "A")
    public void create(String json){
        logger.debug("Creating mimimal user from json...");
        user = createUserFromJson(json);
    }

    @Test(dependsOnMethods = "create")
    public void updateWrongId(){
        user.setActive(true);
        Response response=client.updateUser(user, "not an id", null, null);
        assertNotEquals(response.getStatus(), CREATED.getStatusCode());
    }

    @Test(dependsOnMethods = "updateWrongId", alwaysRun = true)
    public void delete() {
        deleteUser(user);
    }

}
