package io.jans.casa.model;

public class User {

    private String userName;
    private String givenName;
    private String lastName;
    private String preferredMethod;
    private String id;

	public String getUserName() {
        return userName;
    }

    public String getGivenName() {
        return givenName;
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

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
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
    
}