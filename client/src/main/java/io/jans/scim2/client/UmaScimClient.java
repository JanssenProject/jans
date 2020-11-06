/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client;

import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.jans.as.client.TokenRequest;
import io.jans.as.client.uma.UmaClientFactory;
import io.jans.as.client.uma.UmaTokenService;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.token.ClientAssertionType;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.UmaTokenResponse;
import io.jans.scim2.client.exception.ScimInitializationException;
import io.jans.util.StringHelper;

/**
 * Instances of this class contain the necessary logic to handle the authorization processes required by a client of SCIM
 * service in UMA mode of protection. For more information on SCIM protected by UMA 2.0 visit the
 * <a href="https://www.gluu.org/docs/ce/user-management/scim2/">SCIM 2.0 docs page</a>.
 * <p><b>Note:</b> Do not instantiate this class in your code. To interact with the service, call the corresponding method in
 * class {@link io.jans.scim2.client.factory.ScimClientFactory ScimClientFactory} that returns a proxy object wrapping this client
 * @param <T> Type parameter of superclass
 */
/*
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * Updated by jgomer on 2017-10-19
 */
public class UmaScimClient<T> extends AbstractScimClient<T> {

    private static final long serialVersionUID = 7099883500099353832L;

    private Logger logger = LogManager.getLogger(getClass());

    private String rpt;

    private String umaAatClientId;
    private String umaAatClientKeyId;
    private String umaAatClientJksPath;
    private String umaAatClientJksPassword;

    /**
     * Constructs a UmaScimClient object with the specified parameters and service contract
     * @param serviceClass The service interface the underlying resteasy proxy client will adhere to. This proxy is used
     *                     internally to execute all requests to the service
     * @param domain The root URL of the SCIM service. Usually in the form {@code https://your.gluu-server.com/identity/restv1}
     * @param umaAatClientId Requesting party Client Id
     * @param umaAatClientJksPath Path to requesting party jks file in local filesystem
     * @param umaAatClientJksPassword Keystore password
     * @param umaAatClientKeyId Key Id in the keystore. Pass an empty string to use the first key in keystore
     */
    public UmaScimClient(Class<T> serviceClass, String domain, String umaAatClientId, String umaAatClientJksPath,
                         String umaAatClientJksPassword, String umaAatClientKeyId) {
        super(domain, serviceClass);
        this.umaAatClientId = umaAatClientId;
        this.umaAatClientJksPath = umaAatClientJksPath;
        this.umaAatClientJksPassword = umaAatClientJksPassword;
        this.umaAatClientKeyId = umaAatClientKeyId;
    }

    /**
     * Builds a string suitable for being passed as an authorization header. It does so by prefixing the current Requesting
     * Party Token this object has with the word "Bearer ".
     * @return String built or null if this instance has no RPT yet
     */
    @Override
    String getAuthenticationHeader() {
    	return StringHelper.isEmpty(rpt) ?  null : "Bearer " + rpt;
    }

    /**
     * Recomputes a new RPT according to UMA workflow if the response passed as parameter has status code 401 (unauthorized).
     * @param response A Response object corresponding to the request obtained in the previous call to a service method
     * @return If the parameter passed has a status code different to 401, it returns false. Otherwise it returns the success
     * of the attempt made to get a new RPT
     */
    @Override
    boolean authorize(Response response) {

        boolean value = false;

        if (response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()) {

            try {
                String permissionTicketResponse = response.getHeaderString("WWW-Authenticate");
                String permissionTicket = null;
                String asUri = null;

                String[] headerKeyValues = StringHelper.split(permissionTicketResponse, ",");
                for (String headerKeyValue : headerKeyValues) {
                    if (headerKeyValue.startsWith("ticket=")) {
                        permissionTicket = headerKeyValue.substring(7);
                    }
                    if (headerKeyValue.startsWith("as_uri=")) {
                        asUri = headerKeyValue.substring(7);
                    }
                }
                value= StringHelper.isNotEmpty(asUri) && StringHelper.isNotEmpty(permissionTicket)
                        && obtainAuthorizedRpt(asUri, permissionTicket);
            } catch (Exception e) {
                throw new ScimInitializationException(e.getMessage(), e);
            }
        }

        return value;
    }

    private boolean obtainAuthorizedRpt(String asUri, String ticket) {

        try {
            return StringUtils.isNotBlank(getAuthorizedRpt(asUri, ticket));
        } catch (Exception e) {
            throw new ScimInitializationException(e.getMessage(), e);
        }

    }

    private String getAuthorizedRpt(String asUri, String ticket) {

        try {
        	// Get metadata configuration
        	UmaMetadata umaMetadata = UmaClientFactory.instance().createMetadataService(asUri).getMetadata();
            if (umaMetadata == null) {
                throw new ScimInitializationException(String.format("Failed to load valid UMA metadata configuration from: %s", asUri));
            }

        	TokenRequest tokenRequest = getAuthorizationTokenRequest(umaMetadata);
            //No need for claims token. See comments on issue https://github.com/GluuFederation/SCIM-Client/issues/22

            UmaTokenService tokenService = UmaClientFactory.instance().createTokenService(umaMetadata);
            UmaTokenResponse rptResponse = tokenService.requestJwtAuthorizationRpt(ClientAssertionType.JWT_BEARER.toString(),
                    tokenRequest.getClientAssertion(), GrantType.OXAUTH_UMA_TICKET.getValue(), ticket, null, null, null, null, null); //ClaimTokenFormatType.ID_TOKEN.getValue()

            if (rptResponse == null) {
                throw new ScimInitializationException("UMA RPT token response is invalid");
            }

            if (StringUtils.isBlank(rptResponse.getAccessToken())) {
                throw new ScimInitializationException("UMA RPT is invalid");
            }

            this.rpt = rptResponse.getAccessToken();
            return rpt;

        } catch (Exception ex) {
            throw new ScimInitializationException(ex.getMessage(), ex);
        }

    }

    private TokenRequest getAuthorizationTokenRequest(UmaMetadata umaMetadata) {

        try {
            if (StringHelper.isEmpty(umaAatClientJksPath) || StringHelper.isEmpty(umaAatClientJksPassword)) {
                throw new ScimInitializationException("UMA JKS keystore path or password is empty");
            }

            AuthCryptoProvider cryptoProvider;
            try {
                cryptoProvider = new AuthCryptoProvider(umaAatClientJksPath, umaAatClientJksPassword, null);
            } catch (Exception ex) {
                throw new ScimInitializationException("Failed to initialize crypto provider");
            }

            String keyId = umaAatClientKeyId;
            if (StringHelper.isEmpty(keyId)) {
                // Get first key
                List<String> aliases = cryptoProvider.getKeys();
                if (aliases.size() > 0) {
                    keyId = aliases.get(0);
                }
            }

            if (StringHelper.isEmpty(keyId)) {
                throw new ScimInitializationException("UMA keyId is empty");
            }

            TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
            tokenRequest.setAuthUsername(umaAatClientId);
            tokenRequest.setCryptoProvider(cryptoProvider);
            tokenRequest.setAlgorithm(cryptoProvider.getSignatureAlgorithm(keyId));
            tokenRequest.setKeyId(keyId);
            tokenRequest.setAudience(umaMetadata.getTokenEndpoint());

            return tokenRequest;

        } catch (Exception ex) {
            throw new ScimInitializationException("Failed to get client token", ex);
        }

    }

}
