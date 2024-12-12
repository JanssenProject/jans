package io.jans.fido2.model.common;

public class RelyingParty implements PublicKeyCredentialEntity{
    private String id;
    private String name;

    public RelyingParty(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static RelyingParty createRelyingParty(String id, String name) {
        return new RelyingParty(id, name);
    }

    @Override
    public String toString() {
        return "RelyingParty{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
