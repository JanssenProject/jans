package io.jans.as.common.model.authzdetails;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Yuriy Z
 */
public class AuthzDetails {
    private AuthzDetails() {
    }

    private final List<AuthzDetail> details = new ArrayList<>();

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
}
