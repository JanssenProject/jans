package io.jans.casa.core.pojo;

import io.jans.casa.misc.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A java bean representing an end-user. It contains the most common attributes such as id, username, preferred
 * authentication method, etc. Use the setter and getters to manipulate values.
 * @author jgomer
 */
public class User {

    private String userName;
    private String givenName;
    private String lastName;
    private boolean admin;
    private String id;
    private String preferredMethod;
    private String pictureURL;

    private Map<String, Object> claims;

    public void setClaims(Map<String, Object> claims) {

        this.claims = claims;
        setUserName(getClaim("user_name"));
        setPictureURL(getClaim("picture"));
        setLastName(getClaim("family_name"));
        setGivenName(getClaim("given_name"));
        setId(getClaim("inum"));

    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    /**
     * From a collection of claims, it extracts the first value found for a claim whose name is passed. If claim is not
     * found or has an empty list associated, it returns null
     * @param claimName Claim to inspect
     * @return First value of claim or null
     */
    public String getClaim(String claimName) {

        Object values = claims.get(claimName);
        if (values != null) {
            Object value = values;

            if (Collection.class.isAssignableFrom(values.getClass())) {
                List<Object> list = new ArrayList<Object>(Collection.class.cast(values));
                value = Utils.isEmpty(list) ? null : list.get(0);
            }
            return value.toString();
        }
        return null;

    }

	public String getUserName() {
        return userName;
    }

    public String getGivenName() {
        return givenName;
    }

    public boolean isAdmin() {
        return admin;
    }

    public String getPreferredMethod() {
        return preferredMethod;
    }

    public String getId() {
        return id;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPictureURL() {
        return pictureURL;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPreferredMethod(String preferredMethod) {
        this.preferredMethod = preferredMethod;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setPictureURL(String pictureURL) {
        this.pictureURL = pictureURL;
    }

}
