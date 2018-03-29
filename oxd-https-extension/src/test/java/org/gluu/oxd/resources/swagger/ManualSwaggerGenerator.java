package org.gluu.oxd.resources.swagger;

import org.apache.commons.io.IOUtils;
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
