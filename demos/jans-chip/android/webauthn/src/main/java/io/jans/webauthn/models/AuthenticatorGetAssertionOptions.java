package io.jans.webauthn.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import io.jans.webauthn.util.Base64ByteArrayAdapter;

public class AuthenticatorGetAssertionOptions {
    @SerializedName("rpId")
    public String rpId;
    @SerializedName("clientDataHash")
    public byte[] clientDataHash;
    @SerializedName("allowCredentialDescriptorList")
    public List<PublicKeyCredentialDescriptor> allowCredentialDescriptorList;
    @SerializedName("requireUserPresence")
    public boolean requireUserPresence;
    @SerializedName("requireUserVerification")
    public boolean requireUserVerification;
    // TODO: authenticatorExtensions

    public boolean areWellFormed() {
        if (rpId.isEmpty()) {
            return false;
        }
        if (clientDataHash.length != 32) {
            return false;
        }
        if (!(requireUserPresence ^ requireUserVerification)) { // only one may be set
            return false;
        }
        return true;
    }

    public static AuthenticatorGetAssertionOptions fromJSON(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(byte[].class, new Base64ByteArrayAdapter())
                .create();
        return gson.fromJson(json, AuthenticatorGetAssertionOptions.class);
    }
}
