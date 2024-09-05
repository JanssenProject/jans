package io.jans.as.model.authzdetails;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Yuriy Z
 */
public class AuthzDetails {

    private final List<AuthzDetail> details;

    public AuthzDetails(List<AuthzDetail> details) {
        this.details = details;
    }

    public AuthzDetails() {
        this(new ArrayList<>());
    }

    public static AuthzDetails of(String jsonArray) {
        return of(new JSONArray(jsonArray));
    }

    public static AuthzDetails ofSilently(String jsonArray) {
        try {
            return of(new JSONArray(jsonArray));
        } catch (Exception e) {
            return null;
        }
    }

    public static AuthzDetails of(JSONArray jsonArray) {
        AuthzDetails result = new AuthzDetails();
        for (int i = 0; i < jsonArray.length(); i++) {
            result.details.add(new AuthzDetail(jsonArray.getJSONObject(i)));
        }
        return result;
    }

    public static boolean similar(String authorizationDetails1, String authorizationDetails2) {
        if (StringUtils.equals(authorizationDetails1, authorizationDetails2)) {
            return true;
        }
        if (authorizationDetails1 == null || authorizationDetails2 == null) {
            return false;
        }
        JSONArray array1 = new JSONArray(authorizationDetails1);
        JSONArray array2 = new JSONArray(authorizationDetails2);
        return array1.similar(array2);
    }

    public static String simpleMerge(String authorizationDetails1, String authorizationDetails2) {
        final AuthzDetails details1 = AuthzDetails.of(authorizationDetails1);
        final AuthzDetails details2 = AuthzDetails.of(authorizationDetails2);
        details1.getDetails().addAll(details2.getDetails());
        return details1.asJsonArray().toString();
    }

    public JSONArray asJsonArray() {
        JSONArray array = new JSONArray();
        array.putAll(details.stream().map(AuthzDetail::getJsonObject).collect(Collectors.toList()));
        return array;
    }

    public String asJsonString() {
        return asJsonArray().toString();
    }

    public boolean similar(String authorizationDetails) {
        if (StringUtils.isBlank(authorizationDetails)) {
            return false;
        }
        return asJsonArray().similar(new JSONArray(authorizationDetails));
    }

    public List<AuthzDetail> getDetails() {
        return details;
    }

    public Set<String> getTypes() {
        Set<String> result = new HashSet<>();
        for (AuthzDetail d : details) {
            result.add(d.getType());
        }
        return result;
    }

    public static boolean isEmpty(AuthzDetails authzDetails) {
        return authzDetails == null || authzDetails.getDetails() == null || authzDetails.getDetails().isEmpty();
    }

    @Override
    public String toString() {
        return "AuthzDetails{" +
                "details=" + details +
                '}';
    }
}
