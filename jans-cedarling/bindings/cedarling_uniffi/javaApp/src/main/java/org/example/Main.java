package org.example;

import org.example.jwt.JWTCreator;
import org.example.utils.AppUtils;
import org.example.utils.JsonUtil;
import uniffi.mobile.AuthorizeResult;
import uniffi.mobile.Cedarling;
import uniffi.mobile.CedarlingException;
import uniffi.mobile.EntityData;

import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String bootstrap = AppUtils.readFile("./src/main/resources/config/bootstrap.json");
        try {
            Cedarling instance = Cedarling.Companion.loadFromJson(bootstrap);

            JWTCreator jwtCreator = new JWTCreator("-very-strong-shared-secret-of-at-least-32-bytes!");

            String accessTokenJson = AppUtils.readFile("./src/main/resources/config/access_token_payload.json");
            String idTokenJson = AppUtils.readFile("./src/main/resources/config/id_token_payload.json");
            String userInfoJson = AppUtils.readFile("./src/main/resources/config/user_info_payload.json");
            Map<String, String> tokens = Map.of(
                    "access_token",
                    jwtCreator.createJwtFromJson(accessTokenJson),
                    "id_token",
                    jwtCreator.createJwtFromJson(idTokenJson),
                    "userinfo_token",
                    jwtCreator.createJwtFromJson(userInfoJson)
            );

            AuthorizeResult result = instance.authorize(tokens,
                    AppUtils.readFile("./src/main/resources/config/action.txt"),
                    EntityData.Companion.fromJson(AppUtils.readFile("./src/main/resources/config/resource.json")),
                    AppUtils.readFile("./src/main/resources/config/context.json"));

            System.out.println("Decision: " + JsonUtil.toPrettyJson(result.getDecision()));
            System.out.println("Result: " + JsonUtil.toPrettyJson(result));

            List<String> logs = instance.getLogsByRequestId(result.getRequestId());

            System.out.println("============Logs=========");
            logs.forEach(System.out::println);

        } catch (CedarlingException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}