package io.jans.webauthn.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import io.jans.webauthn.util.Base64ByteArrayAdapter;

public class AuthenticatorGetAssertionResult implements JsonSerializable {
    @SerializedName("selected_credential_id")
    public byte[] selectedCredentialId;
    @SerializedName("authenticator_data")
    public byte[] authenticatorData;
    @SerializedName("signature")
    public byte[] signature;
    @SerializedName("selected_credential_user_handle")
    public byte[] selectedCredentialUserHandle;

    public AuthenticatorGetAssertionResult(byte[] selectedCredentialId, byte[] authenticatorData, byte[] signature, byte[] selectedCredentialUserHandle) {
        this.selectedCredentialId = selectedCredentialId;
        this.authenticatorData = authenticatorData;
        this.signature = signature;
        this.selectedCredentialUserHandle = selectedCredentialUserHandle;
    }

    public String toJson() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(byte[].class, new Base64ByteArrayAdapter())
                .disableHtmlEscaping()
                .create();
        return gson.toJson(this);
    }
}
