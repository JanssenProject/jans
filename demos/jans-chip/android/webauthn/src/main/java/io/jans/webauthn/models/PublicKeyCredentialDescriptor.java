package io.jans.webauthn.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PublicKeyCredentialDescriptor {
    @SerializedName("type")
    public String type;
    @SerializedName("id")
    public byte[] id;
    @SerializedName("transports")
    public List<String> transports;

    public PublicKeyCredentialDescriptor(String type, byte[] id, List<String> transports) {
        this.type = type;
        this.id = id;
        this.transports = transports;
    }
}
