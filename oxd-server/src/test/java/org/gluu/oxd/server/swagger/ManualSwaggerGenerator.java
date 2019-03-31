package org.gluu.oxd.server.swagger;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.io.InputStream;
import java.util.Iterator;

/**
 * @author yuriyz
 */
public class ManualSwaggerGenerator {

    public static void main(String[] args) throws Exception {
        InputStream inputStream = ManualSwaggerGenerator.class.getResourceAsStream("/input.json");
        try {
            StringBuilder result = new StringBuilder();
            JSONObject json = new JSONObject(IOUtils.toString(inputStream));
            handleJSONObject(result, json, 12);
            System.out.println(result);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private static void append(StringBuilder result, int indent, String toAppend) {
        for (int i = 0; i < indent; i++) {
            result.append(" ");
        }
        result.append(toAppend);
    }

    private static void handleJSONObject(StringBuilder result, JSONObject json, int indent) {
        append(result, indent, "type: object\n");

        appendRequired(result, json, indent);
        appendProperties(result, json, indent);
    }

    private static void appendProperties(StringBuilder result, JSONObject json, int indent) {
        append(result, indent, "properties:\n");
        indent += 2;
        Iterator keys = json.keys();
        while (keys.hasNext()) {
            String nextKey = (String) keys.next();
            append(result, indent, String.format("%s:\n", nextKey)); // property name
            Object opt = json.opt(nextKey);
            if (opt instanceof String) {
                append(result, indent, "  type: string\n");
            } else if (opt instanceof JSONObject) {
                indent = indent + 2;
                handleJSONObject(result, (JSONObject) opt, indent);
            } else if (opt instanceof Integer || opt instanceof Long) {
                append(result, indent, "  type: integer\n");
            } else if (opt instanceof Boolean) {
                append(result, indent, "  type: boolean\n");
            } else if (opt instanceof Float || opt instanceof Double) {
                append(result, indent, "  type: number\n");
            } else if (opt instanceof JSONArray) {
                append(result, indent, "  type: array\n");
                append(result, indent, "  items:\n");
                append(result, indent, "    type: string\n");
            } else if (opt == null || opt == JSONObject.NULL) {
                append(result, indent, "  type: string\n");
            } else {
                System.out.println(opt);
            }

            switch (nextKey) {
                case "status":
                    append(result, indent, "  example: ok\n");
                    break;
                case "oxd_id":
                    append(result, indent, "  example: bcad760f-91ba-46e1-a020-05e4281d91b6\n");
                    break;
                case "client_id_of_oxd_id":
                    append(result, indent, "  example: ccad760f-91ba-46e1-a020-05e4281d91b6\n");
                    break;
                case "op_host":
                    append(result, indent, "  example: https://<op-hostname>\n");
                    break;
                case "client_id":
                    append(result, indent, "  example: '@!1736.179E.AA60.16B2!0001!8F7C.B9AB!0008!A2BB.9AE6.5F14.B387'\n");
                    break;
                case "client_secret":
                    append(result, indent, "  example: f436b936-03fc-433f-9772-53c2bc9e1c74\n");
                    break;
                case "client_registration_access_token":
                    append(result, indent, "  example: d836df94-44b0-445a-848a-d43189839b17\n");
                    break;
                case "client_registration_client_uri":
                    append(result, indent, "  example: https://<op-hostname>/oxauth/restv1/register?client_id=@!1736.179E.AA60.16B2!0001!8F7C.B9AB!0008!A2BB.9AE6.5F14.B387\n");
                    break;
                case "client_id_issued_at":
                    append(result, indent, "  example: 1501854943\n");
                    break;
                case "client_secret_expires_at":
                    append(result, indent, "  example: 1501941343\n");
                    break;
                case "scope":
                    append(result, indent, "  example: [\"openid\"]\n");
                    break;
                case "authorization_redirect_uri":
                case "post_logout_redirect_uri":
                    append(result, indent, "  example: https://client.example.org/cb\n");
                    break;
                case "application_type":
                    append(result, indent, "  example: web\n");
                    break;
                case "response_types":
                    append(result, indent, "  example: [\"code\"]\n");
                    break;
                case "grant_types":
                    append(result, indent, "  example: [\"authorization_code\", \"client_credentials\"]\n");
                    break;
                case "acr_values":
                    append(result, indent, "  example: [\"basic\"]\n");
                    break;
                case "contacts":
                    append(result, indent, "  example: [\"foo_bar@spam.org\"]\n");
                    break;
                case "access_token":
                    append(result, indent, "  example: b75434ff-f465-4b70-92e4-b7ba6b6c58f2\n");
                    break;
                case "expires_in":
                case "exp":
                    append(result, indent, "  example: 299\n");
                    break;
                case "iat":
                    append(result, indent, "  example: 1419350238\n");
                    break;
                case "aud":
                    append(result, indent, "  example: l238j323ds-23ij4\n");
                    break;
                case "active":
                    append(result, indent, "  example: true\n");
                    break;
                case "username":
                    append(result, indent, "  example: John Black\n");
                    break;
                case "token_type":
                    append(result, indent, "  example: bearer\n");
                    break;
                case "sub":
                    append(result, indent, "  example: jblack\n");
                    break;
                case "iss":
                    append(result, indent, "  example: https://as.gluu.org/\n");
                    break;
                case "extension_field":
                    append(result, indent, "  example: twenty-seven\n");
                    break;
                case "id_token":
                    append(result, indent, "  example: eyJraWQiOiI5MTUyNTU1Ni04YmIwLTQ2MzYtYTFhYy05ZGVlNjlhMDBmYWUiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL2NlLWRldjMuZ2x1dS5vcmciLCJhdWQiOiJAITE3MzYuMTc5RS5BQTYwLjE2QjIhMDAwMSE4RjdDLkI5QUIhMDAwOCE5Njk5LkFFQzcuOTM3MS4yODA3IiwiZXhwIjoxNTAxODYwMzMwLCJpYXQiOjE1MDE4NTY3MzAsIm5vbmNlIjoiOGFkbzJyMGMzYzdyZG03OHU1OTUzbTc5MXAiLCJhdXRoX3RpbWUiOjE1MDE4NTY2NzIsImF0X2hhc2giOiItQ3gyZHo1V3Z3X2tCWEFjVHMzbUZBIiwib3hPcGVuSURDb25uZWN0VmVyc2lvbiI6Im\n");
                    break;
                case "refresh_token":
                    append(result, indent, "  example: 33d7988e-6ffb-4fe5-8c2a-0e158691d446\n");
                    break;
                case "at_hash":
                    append(result, indent, "  example: -Cx2dz5Wvw_kBXAcTs3mFA\n");
                    break;
            }
        }
    }

    private static void appendRequired(StringBuilder result, JSONObject json, int indent) {
        append(result, indent, "required:\n");
        Iterator keys = json.keys();
        while (keys.hasNext()) {
            String nextKey = (String) keys.next();
            append(result, indent, String.format("  - %s\n", nextKey));
        }
    }
}


//type: object
//        required:
//        - status
//        - data
//        properties:
//        status:
//        type: string
//        example: ok
//        data:
//        type: object
//        required:
//        - oxd_id
//        - client_id_of_oxd_id
//        - op_host
//        - setup_client_oxd_id
//        properties:
//        status:
//        type: string
//        example: ok
