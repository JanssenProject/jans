package org.gluu.agama.inji;

import io.jans.orm.exception.operation.EntryNotFoundException;
import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.UserService;
import io.jans.as.common.util.CommonUtils;
import io.jans.as.common.model.registration.Client;

import io.jans.as.common.model.session.SessionId;
import io.jans.as.server.service.SessionIdService;
import jakarta.servlet.http.HttpServletRequest;
import io.jans.service.net.NetworkService;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.agama.engine.script.LogUtils;
import io.jans.util.StringHelper;

import io.jans.as.model.exception.InvalidJwtException;

import io.jans.service.cdi.util.CdiUtil;
import io.jans.as.server.service.ClientService;

import io.jans.util.security.StringEncrypter.EncryptionException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.sql.Timestamp;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.gluu.agama.inji.AgamaInjiVerificationService;

public class AgamaInjiVerificationServiceImpl extends AgamaInjiVerificationService{

    private static final String INUM_ATTR = "inum";
    private static final String UID = "uid";
    private static final String MAIL = "mail";
    private static final String CN = "cn";
    private static final String DISPLAY_NAME = "displayName";
    private static final String GIVEN_NAME = "givenName";
    private static final String SN = "sn";
    private static final String VERIFIABLE_CREDENTIALS = "verifiableCredentials";
    private String USER_INFO_FROM_VC = null;
    private String VERIFIABLE_CREDENTIALS_JSON = null;
    // private String INJI_API_ENDPOINT = "http://mmrraju-comic-pup.gluu.info/backend/consent/new";
    private String INJI_BACKEND_BASE_URL = "https://injiverify.collab.mosip.net";
    private String INJI_WEB_BASE_URL = "https://injiweb.collab.mosip.net";
    private String  CLIENT_ID = "agama-app";
    private Map<String, Object> AUTHORIZATION_DETAILS = new HashMap<>();
    private String NONCE ;
    private String RESPONSE_URL ;

    public  String CALLBACK_URL= ""; // Agama call-back URL
    private String RFAC_DEMO_BASE = "https://mmrraju-adapted-crab.gluu.info/inji-user.html"; // INJI RP URL.
    private HashMap<String, Object> flowConfig ;
    private HashMap<String, Object> PRESENATION_DEFINITION;
    private HashMap<String, Object> CLIENT_METADATA;
    private List<Map<String, Object>> CREDENTIAL_MAPPINGS;
    private HashMap<String, String> VC_TO_GLUU_MAPPING; // Current credential mapping (NID by default)
    private static AgamaInjiVerificationServiceImpl INSTANCE = null;

    public AgamaInjiVerificationServiceImpl(){}

    public AgamaInjiVerificationServiceImpl(HashMap config){
        if(config != null){
            LogUtils.log("Flow config provided is: %", config);
            flowConfig = config;

            this.INJI_BACKEND_BASE_URL = flowConfig.get("injiVerifyBaseURL") !=null ? flowConfig.get("injiVerifyBaseURL").toString() : INJI_BACKEND_BASE_URL;
            this.INJI_WEB_BASE_URL = flowConfig.get("injiWebBaseURL") !=null ? flowConfig.get("injiWebBaseURL").toString() : INJI_WEB_BASE_URL;
            this.CLIENT_ID = flowConfig.get("clientId") != null ? flowConfig.get("clientId").toString() : CLIENT_ID;
            this.PRESENATION_DEFINITION = flowConfig.get("presentationDefinition") !=null ? (HashMap<String, Object>) flowConfig.get("presentationDefinition") : this.getPresentationDefinitionSample();
            this.CLIENT_METADATA = flowConfig.get("clientMetadata")  !=null ? (HashMap<String, Object>) flowConfig.get("clientMetadata") : this.buildClientMetadata();
            this.CALLBACK_URL = flowConfig.get("agamaCallBackUrl") != null ? flowConfig.get("agamaCallBackUrl").toString() : CALLBACK_URL;
            
            // Load credential mappings list
            this.CREDENTIAL_MAPPINGS = flowConfig.get("credentialMappings") != null ? 
                (List<Map<String, Object>>) flowConfig.get("credentialMappings") : new ArrayList<>();
            
            // Set default mapping to NID (first in list or fallback)
            if (!this.CREDENTIAL_MAPPINGS.isEmpty()) {
                Map<String, Object> nidMapping = this.CREDENTIAL_MAPPINGS.get(0);
                this.VC_TO_GLUU_MAPPING = (HashMap<String, String>) nidMapping.get("vcToGluuMapping");
                LogUtils.log("Loaded credential mapping for type: %", nidMapping.get("credentialType"));
            } else {
                // Fallback to old config format for backward compatibility
                this.VC_TO_GLUU_MAPPING = flowConfig.get("vcToGluuMapping") != null ? 
                    (HashMap<String, String>) flowConfig.get("vcToGluuMapping") : new HashMap<>();
                LogUtils.log("Using legacy vcToGluuMapping configuration");
            }
        }else{
            LogUtils.log("Error: No configuration provided using default may not work properly...");
        }


    }

    public static synchronized AgamaInjiVerificationServiceImpl getInstance(HashMap config)
    {
        
        if (INSTANCE == null)
            INSTANCE = new AgamaInjiVerificationServiceImpl(config);
        return INSTANCE;
    } 

    @Override
    public Map<String, Object> createVpVerificationRequest() {

        Map<String, Object> responseMap = new HashMap<>();

        try {
            // LogUtils.log("Retrieve  session...");
            Map<String, String> sessionAttrs = getSessionId().getSessionAttributes();

            LogUtils.log(sessionAttrs);
            String clientId = sessionAttrs.get("client_id");
            // this.CLIENT_ID = clientId;
            LogUtils.log("Create VP Verification Request...");
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("clientId", CLIENT_ID);
            requestPayload.put("presentationDefinition", PRESENATION_DEFINITION);
            String jsonPayload = new ObjectMapper().writeValueAsString(requestPayload);
            LogUtils.log("Payload object: %", requestPayload);
            LogUtils.log("Payload JSON: %", jsonPayload);
            String endpoint = this.INJI_BACKEND_BASE_URL + "/v1/verify/vp-request";

            
            HttpClient httpClient = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL) 
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Cache-Control", "no-cache")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                String jsonResponse = response.body();
                LogUtils.log("INJI Verify Backend Response: %", jsonResponse);
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> data = mapper.readValue(jsonResponse, Map.class);

                if (data == null || !data.containsKey("requestId") || !data.containsKey("transactionId")) {
                    LogUtils.log("ERROR: Missing Data from INJI backend Response response");
                    responseMap.put("valid", false);
                    responseMap.put("message", "ERROR: Missing Data from INJI Verify backend response");
                }  
                String transactionId = (String) data.get("transactionId");
                String requestId = (String) data.get("requestId");
                this.AUTHORIZATION_DETAILS = (Map<String, Object>) data.get("authorizationDetails");
                LogUtils.log("Authorization details : %", this.AUTHORIZATION_DETAILS);
                responseMap.put("valid", true);
                responseMap.put("message", "INJI Verify Backed System response is satisfy");
                responseMap.put("requestId", requestId);
                responseMap.put("transactionId", transactionId);
                return responseMap;               
           
            }else{
                LogUtils.log("ERROR: INJI Verify returned status code: %", response.statusCode());
                responseMap.put("valid", false);

                responseMap.put("message", "ERROR: INJI BACKEND returned status code: % "+response.statusCode());
                return responseMap;
            }


        } catch (Exception e) {
            responseMap.put("valid", false);
            responseMap.put("message", e.getMessage());
        }

        return responseMap;
    }


    @Override
    public String buildInjiWebAuthorizationUrl(String requestId, String transactionId) {
        try {
            LogUtils.log("Preparing Inji web Authorization Url...");

            String nonce = this.AUTHORIZATION_DETAILS.get("nonce").toString();
            // LogUtils.log("NONCE : %", nonce);
            String baseUrl = this.INJI_WEB_BASE_URL + "/authorize";

            String presentationDefinitionJson = new JSONObject(this.AUTHORIZATION_DETAILS.get("presentationDefinition")).toString();
            // LogUtils.log("Presentation defenation: %", presentationDefinitionJson);
            String clientMetadataJson = new JSONObject(this.CLIENT_METADATA).toString();

            // LogUtils.log(clientMetadataJson);
            String url = baseUrl +
                    "?client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8) +
                    "&presentation_definition=" + URLEncoder.encode(presentationDefinitionJson, StandardCharsets.UTF_8) +
                    "&nonce=" + URLEncoder.encode(nonce, StandardCharsets.UTF_8) +
                    "&response_uri=" + URLEncoder.encode((String) this.AUTHORIZATION_DETAILS.get("responseUri"), StandardCharsets.UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(this.CALLBACK_URL, StandardCharsets.UTF_8) +
                    "&response_type=" +this.AUTHORIZATION_DETAILS.get("responseType")  +
                    "&response_mode=" + this.AUTHORIZATION_DETAILS.get("responseMode") +
                    "&client_id_scheme=pre-registered" +
                    "&state=" + URLEncoder.encode(requestId, StandardCharsets.UTF_8) +
                    "&client_metadata=" + URLEncoder.encode(clientMetadataJson, StandardCharsets.UTF_8);

            LogUtils.log("URL : %", url);
            return url;
            // return RFAC_DEMO_BASE;

        } catch (Exception e) {
            LogUtils.log("ERROR: Failed to build Inji Web Authorization URL: %", e.getMessage());
            return null;
        }
    }


    @Override
    public Map<String, Object> verifyInjiAppResult(String requestId, String transactionId) {
        Map<String, Object> response = new HashMap<>();

        LogUtils.log("INJI user back to agama...");

        LogUtils.log("Data : requestId : % transactionId : %", requestId, transactionId);

        String requestIdStatus = checkRequestIdStatus(requestId);

        if (!"VP_SUBMITTED".equals(requestIdStatus)) {
            response.put("valid", false);
            response.put("message", "Error: VP REQUEST ID STATUS is " + requestIdStatus);
            return response;
        }

        String transactionIdStatus = checkTransactionIdStatus(transactionId);

        if (!"SUCCESS".equals(transactionIdStatus)) {
            response.put("valid", false);
            response.put("message", "Error: No VP submission found for given transaction ID " + transactionIdStatus);
            return response;
        }

        response.put("valid", true);
        response.put("message", "VP TOKEN Verification successful");
        return response;

    }    
    
    private String checkTransactionIdStatus(String transactionId) {
        try {
            LogUtils.log("Validating VP TRANSACTION ID STATUS for : %", transactionId);
            String apiUrl = this.INJI_BACKEND_BASE_URL + "/v1/verify/vp-result/" + transactionId;
            // String apiUrl = "http://mmrraju-comic-pup.gluu.info/account-access-consents/" + "intent-id-123456";

            HttpClient httpClient = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Cache-Control", "no-cache")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // LogUtils.log("INJI VERIFY BACKEND RESPONSE FOR TRANSACTION-ID : %", response.body());
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> data = mapper.readValue(response.body(), Map.class);
                // Map<String, Object> data = (Map<String, Object>) mapData.get("Data");

                if (data != null || data.containsKey("vpResultStatus")) {
                    List<Map<String, Object>> vcResults = (List<Map<String, Object>>) data.get("vcResults");
                    String vc = (String) vcResults.get(0).get("vc");
                    this.USER_INFO_FROM_VC = vc;
                    LogUtils.log("INJI : VC info -- %", vc);
                    // Store verifiable credentials as JSON
                    this.VERIFIABLE_CREDENTIALS_JSON = buildVerifiableCredentialsJson(vcResults);
                    LogUtils.log("Stored verifiable credentials JSON: %", this.VERIFIABLE_CREDENTIALS_JSON);
                    
                    return data.get("vpResultStatus").toString();
                } else {
                    return "UNKNOWN";
                }
            }else{
                LogUtils.log("ERROR: INJI VP TOKEN FOR TRANSACTION ID status code: %", response.statusCode());
                return "UNKNOWN";
            }

            

        } catch (Exception e) {
            LogUtils.log("ERROR: Exception in checkTransactionIdStatus: %", e.getMessage());
            return "UNKNOWN";
        }
    }

    private String checkRequestIdStatus(String requestId) {
        try {

            LogUtils.log("Validating VP REQUEST STATUS for : %", requestId);
            String apiUrl = this.INJI_BACKEND_BASE_URL + "/v1/verify/vp-request/" + requestId + "/status";
            HttpClient httpClient = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Cache-Control", "no-cache")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode()== 200) {
                LogUtils.log("INJI VERIFY BACKEND RESPONSE FOR REQUEST-ID : %", response.body());
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> data = mapper.readValue(response.body(), Map.class);

                if (data != null || data.containsKey("status")) {
                    LogUtils.log("VP REQUEST STATUS : %", data.get("status") );
                    return data.get("status").toString();
                } else {
                    return "UNKNOWN";
                }
            }else{
                LogUtils.log("ERROR: VP Request status code: %", response.statusCode());
                return "UNKNOWN";
            }
        } catch (Exception e) {
            LogUtils.log("ERROR: Exception in GET VP Request STATUS: %", e.getMessage());
            return "UNKNOWN";
        }
    }

    private SessionId getSessionId() {
        SessionIdService sis = CdiUtil.bean(SessionIdService.class); 
        return sis.getSessionId(CdiUtil.bean(HttpServletRequest.class));
    }   
    

    private HashMap<String, Object> getPresentationDefinitionSample(){

            Map<String, Object> presentationDefinition = new HashMap<>();
            presentationDefinition.put("id", "c4822b58-7fb4-454e-b827-f8758fe27f9a");
            presentationDefinition.put(
                    "purpose",
                    "Relying party is requesting your digital ID for the purpose of Self-Authentication"
            );

            presentationDefinition.put(
                    "format",
                    Map.of(
                            "ldp_vc",
                            Map.of("proof_type", new String[]{"Ed25519Signature2020"})
                    )
            );

            presentationDefinition.put(
                    "input_descriptors",
                    new Object[]{
                            Map.of(
                                    "id", "id card credential",
                                    "format", Map.of(
                                            "ldp_vc",
                                            Map.of("proof_type", new String[]{"RsaSignature2018"})
                                    ),
                                    "constraints", Map.of(
                                            "fields", new Object[]{
                                                    Map.of(
                                                            "path", List.of('$.type'),
                                                            "filter", Map.of(
                                                                    "type", "object",
                                                                    "pattern", "MOSIPVerifiableCredential"
                                                            )
                                                    )
                                            }
                                    )
                            )
                    }
            );   
            return presentationDefinition;     
    }

    private HashMap<String, Object> buildClientMetadata() {

        HashMap<String, Object> clientMetadata = new HashMap<>();

        clientMetadata.put("client_name", "Agama Application");
        clientMetadata.put("logo_uri",
                "https://mosip.github.io/inji-config/logos/StayProtectedInsurance.png");

        HashMap<String, Object> ldpVp = new HashMap<>();
        ldpVp.put("proof_type", List.of(
                "Ed25519Signature2018",
                "Ed25519Signature2020",
                "RsaSignature2018"
        ));

        HashMap<String, Object> vpFormats = new HashMap<>();
        vpFormats.put("ldp_vp", ldpVp);

        clientMetadata.put("vp_formats", vpFormats);

        return clientMetadata;
    }

    @Override
    public Map<String, String> extractUserInfoFromVC() {
        LogUtils.log("INJI : Extract user info from VC : %", USER_INFO_FROM_VC);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> gluuAttrs = new HashMap<>();

        if (USER_INFO_FROM_VC == null) {
            LogUtils.log("Error: No user info found from VC");
            return gluuAttrs;
        }

        try {
            Map<String, Object> vcMap =
                    mapper.readValue(USER_INFO_FROM_VC, Map.class);

            Map<String, Object> credentialSubject =
                    (Map<String, Object>) vcMap.get("credentialSubject");

            if (credentialSubject == null) {
                LogUtils.log("Error: credentialSubject missing in VC");
                return gluuAttrs;
            }

            for (Map.Entry<String, String> entry : VC_TO_GLUU_MAPPING.entrySet()) {

                String vcClaimName = entry.getKey();
                String gluuAttrName = entry.getValue();

                if (credentialSubject.containsKey(vcClaimName)) {

                    Object vcValue = credentialSubject.get(vcClaimName);
                    String normalizedValue = extractVcValue(vcValue);

                    if (normalizedValue != null) {
                        gluuAttrs.put(gluuAttrName, normalizedValue);
                    }
                }
            }

        } catch (Exception e) {
            LogUtils.log("Error parsing VC user info: %", e.getMessage());
        }

        return gluuAttrs;
    }

    @Override
    public Map<String, String> checkUserExists(String email) {
        if (email == null || !email.contains("@")) {
            LogUtils.log("Error: Invalid email provided");
            return null;
        }

        User user = getUser(MAIL, email);
        
        if (user == null) {
            LogUtils.log("No existing user found for email: %", email);
            return null;
        }

        LogUtils.log("Found existing user for email: %", email);
        
        String uid = getSingleValuedAttr(user, UID);
        String inum = getSingleValuedAttr(user, INUM_ATTR);
        String displayName = getSingleValuedAttr(user, DISPLAY_NAME);
        String givenName = getSingleValuedAttr(user, GIVEN_NAME);
        
        if (givenName == null) {
            givenName = displayName;
            if (givenName == null) {
                givenName = email.substring(0, email.indexOf("@"));
            }
        }
        
        // Handle verifiable credentials update
        if (this.VERIFIABLE_CREDENTIALS_JSON != null) {
            String existingCredentials = getSingleValuedAttr(user, VERIFIABLE_CREDENTIALS);
            String mergedCredentials = mergeVerifiableCredentials(existingCredentials, this.VERIFIABLE_CREDENTIALS_JSON);
            
            // Update user with merged credentials
            user.setAttribute(VERIFIABLE_CREDENTIALS, mergedCredentials);
            
            try {
                UserService userService = CdiUtil.bean(UserService.class);
                userService.updateUser(user);
                LogUtils.log("Updated verifiable credentials for existing user: %", email);
            } catch (Exception e) {
                LogUtils.log("Error updating user credentials: %", e.getMessage());
            }
        }
        
        Map<String, String> result = new HashMap<>();
        result.put(UID, uid);
        result.put(INUM_ATTR, inum);
        result.put(MAIL, email);
        result.put(DISPLAY_NAME, displayName);
        result.put(GIVEN_NAME, givenName);
        
        return result;
    }

    @Override
    public Map<String, String> onboardUser(Map<String, String> userInfo, String password) {
        try {
            Map<String, String> gluuAttrs = userInfo;
            LogUtils.log("User registration data: %", gluuAttrs);

            if (gluuAttrs.isEmpty()) {
                LogUtils.log("Error: No user data provided");
                return Collections.emptyMap();
            }

            String email = gluuAttrs.get("mail");
            if (email == null || !email.contains("@")) {
                LogUtils.log("Error: Email missing or invalid");
                return Collections.emptyMap();
            }

            if (password == null || password.isEmpty()) {
                LogUtils.log("Error: Password is required");
                return Collections.emptyMap();
            }

            User newUser = new User();
            String uid = email;  // Use full email as UID
            newUser.setAttribute(UID, uid);
            
            // Set password
            newUser.setAttribute("userPassword", password);
            
            // Set all attributes from gluuAttrs dynamically
            for (Map.Entry<String, String> entry : gluuAttrs.entrySet()) {
                String attrName = entry.getKey();
                String attrValue = entry.getValue();

                if (UID.equals(attrName) || "password".equals(attrName) || "confirmPassword".equals(attrName)) continue;
                if (VERIFIABLE_CREDENTIALS.equals(attrName)) continue; // Skip, will handle separately
                
                if ("birthdate".equals(attrName)) {
                    try {
                        LocalDate localDate = LocalDate.parse(attrValue.replace('/', '-'));
                        LocalDateTime localDateTime = localDate.atStartOfDay();
                        newUser.setAttribute(attrName, Timestamp.valueOf(localDateTime));
                    } catch (DateTimeParseException e) {
                        LogUtils.log("Warning: Invalid birthdate format: %", attrValue);
                    }
                } else {
                    newUser.setAttribute(attrName, attrValue);
                }
            }
            
            if (gluuAttrs.get(DISPLAY_NAME) != null) {
                newUser.setAttribute(GIVEN_NAME, gluuAttrs.get(DISPLAY_NAME));
            }
            
            // Store verifiable credentials JSON if available
            if (this.VERIFIABLE_CREDENTIALS_JSON != null) {
                newUser.setAttribute(VERIFIABLE_CREDENTIALS, this.VERIFIABLE_CREDENTIALS_JSON);
                LogUtils.log("Added verifiable credentials to user profile");
            }
            LogUtils.log("Final USER : % ", newUser);
            UserService userService = CdiUtil.bean(UserService.class);
            newUser = userService.addUser(newUser, true);

            if (newUser == null) {
                LogUtils.log("Error: Failed to add user");
                return Collections.emptyMap();
            }

            LogUtils.log("New user added");

            // User updateUser = getUser(MAIL, email);
            
            // updateUser.setAttribute("userPassword", password);

            // updatedUser = userService.updateUser(updateUser);
            
            // LogUtils.log("User update with password : %", updatedUser);

            String inum = getSingleValuedAttr(newUser, INUM_ATTR);
            String firstName = getSingleValuedAttr(newUser, GIVEN_NAME);

            Map<String, String> result = new HashMap<>(gluuAttrs);
            result.put(UID, uid);
            result.put(INUM_ATTR, inum);
            result.put(GIVEN_NAME, firstName);
            return result;

        } catch (Exception e) {
            LogUtils.log("Error : %", e);
        }
        
    }

    private String extractVcValue(Object vcValue) {

        if (vcValue == null) {
            return null;
        }

        if (vcValue instanceof String) {
            return (String) vcValue;
        }

        if (vcValue instanceof List) {
            List<?> list = (List<?>) vcValue;

            if (!list.isEmpty() && list.get(0) instanceof Map) {
                Map<String, Object> obj =
                        (Map<String, Object>) list.get(0);

                Object value = obj.get("value");
                if (value != null) {
                    return value.toString();
                }
            }
        }

        // Case 3: Fallback
        return vcValue.toString();
    }

    private String parseBirthdate(String dob) {
        if (dob == null || dob.isBlank()) {
            return null;
        }
        return dob.replace('/', '-');
        // LocalDate localDate = LocalDate.parse(dob.replace('/', '-')); // parses yyyy-MM-dd
        // return Timestamp.valueOf(localDate);
    }

    private static User getUser(String attributeName, String value) {
        UserService userService = CdiUtil.bean(UserService.class);
        return userService.getUserByAttribute(attributeName, value, true);
    }   

    private String buildVerifiableCredentialsJson(List<Map<String, Object>> vcResults) {
        if (vcResults == null || vcResults.isEmpty()) {
            return null;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> credentialsMap = new HashMap<>();
            
            for (int i = 0; i < vcResults.size(); i++) {
                Map<String, Object> vcItem = vcResults.get(i);
                String vcString = (String) vcItem.get("vc");
                
                if (vcString != null) {
                    // Parse the VC JSON string into a JSON object
                    Map<String, Object> vcObject = mapper.readValue(vcString, Map.class);
                    
                    // Detect credential type by checking credentialSubject
                    String credentialType = detectCredentialType(vcObject);
                    
                    credentialsMap.put(credentialType, vcObject);
                    
                    LogUtils.log("Processed verifiable credential %: type=%", i, credentialType);
                }
            }
            
            // Convert the map to JSON string
            return mapper.writeValueAsString(credentialsMap);
            
        } catch (Exception e) {
            LogUtils.log("Error building verifiable credentials JSON: %", e.getMessage());
            return null;
        }
    }

    private String detectCredentialType(Map<String, Object> vcObject) {
        try {
            Map<String, Object> credentialSubject = (Map<String, Object>) vcObject.get("credentialSubject");
            
            if (credentialSubject != null) {
                // Check if it has UIN field - it's NID
                if (credentialSubject.containsKey("UIN")) {
                    return "NID";
                }
                // Check if it has tax-related fields - it's TAX
                if (credentialSubject.containsKey("taxId") || credentialSubject.containsKey("taxNumber")) {
                    return "TAX";
                }
            }
            
            // Fallback: check credential type in VC
            Object typeObj = vcObject.get("type");
            if (typeObj instanceof List) {
                List<?> types = (List<?>) typeObj;
                for (Object type : types) {
                    String typeStr = type.toString();
                    if (typeStr.contains("NationalID") || typeStr.contains("NID")) {
                        return "NID";
                    }
                    if (typeStr.contains("Tax")) {
                        return "TAX";
                    }
                }
            }
            
        } catch (Exception e) {
            LogUtils.log("Error detecting credential type: %", e.getMessage());
        }
        
        // Default fallback
        return "UNKNOWN_" + System.currentTimeMillis();
    }

    private String mergeVerifiableCredentials(String existingJson, String newJson) {
        if (existingJson == null || existingJson.isEmpty()) {
            return newJson;
        }
        
        if (newJson == null || newJson.isEmpty()) {
            return existingJson;
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            
            // Parse existing and new credentials
            Map<String, Object> existingMap = mapper.readValue(existingJson, Map.class);
            Map<String, Object> newMap = mapper.readValue(newJson, Map.class);
            
            // Merge: new credentials override existing ones with same key
            existingMap.putAll(newMap);
            
            LogUtils.log("Merged credentials. Total types: %", existingMap);
            
            return mapper.writeValueAsString(existingMap);
            
        } catch (Exception e) {
            LogUtils.log("Error merging credentials: %. Using new credentials only.", e.getMessage());
            return newJson;
        }
    }

    private String getSingleValuedAttr(User user, String attribute) {

        Object value = null;
        if (attribute.equals(UID)) {
            //user.getAttribute("uid", true, false) always returns null :(
            value = user.getUserId();
        } else {
            value = user.getAttribute(attribute, true, false);
        }
        
        // Handle JSONB columns - convert to string if needed
        if (value != null && VERIFIABLE_CREDENTIALS.equals(attribute)) {
            if (value instanceof String) {
                return (String) value;
            } else {
                // If it's returned as a Map or other object, convert to JSON string
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    return mapper.writeValueAsString(value);
                } catch (Exception e) {
                    LogUtils.log("Error converting JSONB to string: %", e.getMessage());
                    return value.toString();
                }
            }
        }
        
        return value == null ? null : value.toString();

    }

    public boolean removeCredentialType(String email, String credentialType) {
        if (email == null || !email.contains("@")) {
            LogUtils.log("Error: Invalid email provided");
            return false;
        }

        if (credentialType == null || credentialType.isEmpty()) {
            LogUtils.log("Error: Credential type is required");
            return false;
        }

        try {
            User user = getUser(MAIL, email);
            
            if (user == null) {
                LogUtils.log("Error: User not found for email: %", email);
                return false;
            }

            String existingCredentials = getSingleValuedAttr(user, VERIFIABLE_CREDENTIALS);
            
            if (existingCredentials == null || existingCredentials.isEmpty()) {
                LogUtils.log("No credentials found for user: %", email);
                return false;
            }

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> credentialsMap = mapper.readValue(existingCredentials, Map.class);
            
            if (!credentialsMap.containsKey(credentialType)) {
                LogUtils.log("Credential type '%' not found for user: %", credentialType, email);
                return false;
            }

            // Remove the specified credential type
            credentialsMap.remove(credentialType);
            LogUtils.log("Removed credential type '%' for user: %", credentialType, email);

            // Update user with remaining credentials
            if (credentialsMap.isEmpty()) {
                // If no credentials left, set to null or empty JSON object
                user.setAttribute(VERIFIABLE_CREDENTIALS, "{}");
                LogUtils.log("No credentials remaining, set to empty object");
            } else {
                String updatedCredentials = mapper.writeValueAsString(credentialsMap);
                user.setAttribute(VERIFIABLE_CREDENTIALS, updatedCredentials);
                LogUtils.log("Updated credentials. Remaining types: %", credentialsMap.keySet());
            }

            UserService userService = CdiUtil.bean(UserService.class);
            userService.updateUser(user);
            
            return true;
            
        } catch (Exception e) {
            LogUtils.log("Error removing credential type: %", e.getMessage());
            return false;
        }
    }
}