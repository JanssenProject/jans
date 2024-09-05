package io.jans.webauthn.models;

import android.util.Base64;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.jans.webauthn.util.Base64ByteArrayAdapter;
import rocks.xmpp.precis.PrecisProfile;
import rocks.xmpp.precis.PrecisProfiles;

public class AuthenticatorMakeCredentialOptions {
    @SerializedName("clientDataHash")
    public byte[] clientDataHash;
    @SerializedName("rp")
    public RpEntity rpEntity;
    @SerializedName("user")
    public UserEntity userEntity;
    @SerializedName("requireResidentKey")
    public boolean requireResidentKey;
    @SerializedName("requireUserPresence")
    public boolean requireUserPresence;
    @SerializedName("requireUserVerification")
    public boolean requireUserVerification;
    @SerializedName("credTypesAndPubKeyAlgs")
    public List<Pair<String, Long>> credTypesAndPubKeyAlgs;
    @SerializedName("excludeCredentials")
    public List<PublicKeyCredentialDescriptor> excludeCredentialDescriptorList;
    // TODO: possibly support extensions in the future
    // @SerializedName("authenticatorExtensions") public byte[] extensions;

    public boolean areWellFormed() {
        PrecisProfile profile = PrecisProfiles.USERNAME_CASE_PRESERVED;
        if (clientDataHash.length != 32) {
            return false;
        }
        if (rpEntity.id.isEmpty()) {
            return false;
        }
        try {
            profile.enforce(rpEntity.name);
            profile.enforce(userEntity.name);
        } catch (Exception e) {
            return false;
        }
        if (userEntity.id.length <= 0 || userEntity.id.length > 64) {
            return false;
        }
        if (!(requireUserPresence ^ requireUserVerification)) { // only one may be set
            return false;
        }
        if (credTypesAndPubKeyAlgs.isEmpty()) {
            return false;
        }
        return true;
    }

    public static AuthenticatorMakeCredentialOptions fromJSON(String json) {
        TypeToken<List<Pair<String, Long>>> credTypesType = new TypeToken<List<Pair<String, Long>>>() {
        };
        TypeToken<List<PublicKeyCredentialDescriptor>> excludeListType = new TypeToken<List<PublicKeyCredentialDescriptor>>() {
        };
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(byte[].class, new Base64ByteArrayAdapter())
                .registerTypeAdapter(credTypesType.getType(), new CredTypesDeserializer())
                .registerTypeAdapter(excludeListType.getType(), new ExcludeCredentialListDeserializer())
                .create();
        return gson.fromJson(json, AuthenticatorMakeCredentialOptions.class);
    }

    private static class CredTypesDeserializer implements JsonDeserializer<List<Pair<String, Long>>> {
        @Override
        public List<Pair<String, Long>> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            List<Pair<String, Long>> credTypes = new ArrayList<>();
            for (JsonElement element : json.getAsJsonArray()) {
                // all elements are arrays like ["public-key", "-7"]
                JsonArray pair = element.getAsJsonArray();
                String type = pair.get(0).getAsString();
                try {
                    long alg = Long.parseLong(pair.get(1).getAsString());
                    credTypes.add(new Pair<>(type, alg));
                } catch (NumberFormatException e) {
                    continue;
                }
            }
            return credTypes;
        }
    }

    private static class ExcludeCredentialListDeserializer implements JsonDeserializer<List<PublicKeyCredentialDescriptor>> {
        @Override
        public List<PublicKeyCredentialDescriptor> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            List<PublicKeyCredentialDescriptor> excludeList = new ArrayList<>();
            if (json.isJsonArray()) {
                for (JsonElement element : json.getAsJsonArray()) {
                    // elements are JSON objects that take the form:
                    // {"type": "public-key", "id": "<base64-bytes>", "transports": ["usb", "nfc", "ble", "internal"] }
                    if (element.isJsonObject()) {
                        JsonObject entryObject = element.getAsJsonObject();
                        String type = entryObject.get("type").getAsString();
                        String idString = entryObject.get("id").getAsString();
                        byte[] id = Base64.decode(idString, Base64.NO_WRAP);
                        List<String> transports = new ArrayList<>();
                        // "transports" is an optional member
                        if (entryObject.has("transports")) {
                            for (JsonElement transport : entryObject.getAsJsonArray("transports")) {
                                transports.add(transport.getAsString());
                            }
                        }
                        excludeList.add(new PublicKeyCredentialDescriptor(type, id, transports));
                    }
                }
            }
            return excludeList;
        }
    }
}

