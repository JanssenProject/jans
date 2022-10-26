package io.jans.as.common.util;

import com.google.common.base.Strings;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.util.JwtUtil;
import org.json.JSONObject;

/**
 * @author Yuriy Zabrovarnyy
 */
public class CommonUtils {

    private CommonUtils() {
    }

    public static JSONObject getJwks(Client client) {
        return Strings.isNullOrEmpty(client.getJwks())
                ? JwtUtil.getJSONWebKeys(client.getJwksUri())
                : new JSONObject(client.getJwks());
    }
}
