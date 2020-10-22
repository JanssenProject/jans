package io.jans.configapi.auth;

import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.service.ConfigurationService;
import io.jans.configapi.service.UmaClientService;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@Named("patService")
public class PatService {

    @Inject
    Logger log;

    @Inject
    ConfigurationService configurationService;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    UmaClientService umaService;

    private Token umaPat;
    private long umaPatAccessTokenExpiration = 0L; // When the "accessToken" will expire;
    private final ReentrantLock lock = new ReentrantLock();

    public Token getPatToken() throws Exception {
        if (isValidPatToken()) {
            return this.umaPat;
        }

        lock.lock();
        try {
            if (isValidPatToken()) {
                return this.umaPat;
            }

            retrievePatToken();
        } finally {
            lock.unlock();
        }

        return this.umaPat;
    }

    private void retrievePatToken() throws Exception {
        this.umaPat = null;

        final UmaMetadata umaMetadata = umaService.getUmaMetadata();
        if (umaMetadata == null) {
            return;
        }
        log.debug("\n\n getClientKeyStoreFile() = " + getClientKeyStoreFile() + " , getClientKeyStorePassword() = "
                + getClientKeyStorePassword() + " , getClientId() =" + getClientId() + " , getClientKeyId() = "
                + getClientKeyId() + "\n\n");
        String umaClientKeyStoreFile = getClientKeyStoreFile();
        String umaClientKeyStorePassword = getClientKeyStorePassword();
        if (StringHelper.isEmpty(umaClientKeyStoreFile) || StringHelper.isEmpty(umaClientKeyStorePassword)) {
            throw new Exception("UMA JKS keystore path or password is empty");
        }
        /*
         * if (umaClientKeyStorePassword != null) { try { umaClientKeyStorePassword =
         * encryptionService.decrypt(umaClientKeyStorePassword); } catch
         * (EncryptionException ex) {
         * logger.error("Failed to decrypt UmaClientKeyStorePassword password", ex); } }
         */
        this.umaPat = UmaClient.requestPat(umaMetadata.getTokenEndpoint(), umaClientKeyStoreFile,
                umaClientKeyStorePassword, getClientId(), getClientKeyId());
        if (this.umaPat == null) {
            this.umaPatAccessTokenExpiration = 0L;
        } else {
            this.umaPatAccessTokenExpiration = (long) umaPat.getExpiresIn() * 1000L;
        }

        if ((this.umaPat == null) || (this.umaPat.getAccessToken() == null)) {
            throw new Exception("Failed to obtain valid UMA PAT token");
        }
    }

    private boolean isValidPatToken() {
        final long now = System.currentTimeMillis();

        // Get new access token only if is the previous one is missing or expired
        return !((umaPat == null) || (umaPat.getAccessToken() == null)
                || (umaPatAccessTokenExpiration <= now));
    }


    protected String getClientKeyStorePassword() {
        return configurationService.find().getKeyStoreSecret();
    }

    protected String getClientKeyStoreFile() {
        return configurationService.find().getKeyStoreFile();
    }

    private String getClientId() {
        return configurationFactory.getApiClientId(); // TBD
    }

    private String getClientKeyId() {
        return null; // TBD
    }
}
