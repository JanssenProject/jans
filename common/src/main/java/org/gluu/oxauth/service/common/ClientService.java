package org.gluu.oxauth.service.common;

import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.common.OxAuthApplicationType;
import org.gluu.oxauth.model.common.OxAuthSubjectType;
import org.gluu.oxauth.model.common.SignatureAlgorithm;
import org.gluu.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.service.OrganizationService;
import org.gluu.oxauth.util.OxConstants;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.search.filter.Filter;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.List;

/**
 * @author gasmyr on 9/17/20.
 */
public class ClientService implements Serializable {

    private static final long serialVersionUID = 7912416439116338984L;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;


    @Inject
    private Logger logger;

    @Inject
    private EncryptionService encryptionService;

    @Inject
    private OrganizationService organizationService;


    @Inject
    private IdGenService idGenService;

    public boolean contains(String clientDn) {
        return persistenceEntryManager.contains(clientDn, Client.class);
    }

    public void addClient(Client client) {
        persistenceEntryManager.persist(client);
    }

    public void removeClient(Client client) {
        persistenceEntryManager.removeRecursively(client.getDn());
    }

    public String getDnForClient(String inum) {
        String orgDn = organizationService.getDnForOrganization();
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=clients,%s", orgDn);
        }
        return String.format("inum=%s,ou=clients,%s", inum, orgDn);
    }

    public void updateClient(Client client) {
        persistenceEntryManager.merge(client);
    }

    public String generateInumForNewClient() {
        String newInum = null;
        String newDn = null;
        int trycount = 0;
        do {
            if (trycount < IdGenService.MAX_IDGEN_TRY_COUNT) {
                newInum = idGenService.generateId("client");
                trycount++;
            } else {
                newInum = idGenService.generateDefaultId();
            }
            newDn = getDnForClient(newInum);
        } while (persistenceEntryManager.contains(newDn, Client.class));
        return newInum;
    }

    public List<Client> searchClients(String pattern, int sizeLimit) {
        String[] targetArray = new String[]{pattern};
        Filter displayNameFilter = Filter.createSubstringFilter(OxConstants.displayName, null, targetArray, null);
        Filter descriptionFilter = Filter.createSubstringFilter(OxConstants.description, null, targetArray, null);
        Filter inumFilter = Filter.createSubstringFilter(OxConstants.inum, null, targetArray, null);
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter, inumFilter);
        return persistenceEntryManager.findEntries(getDnForClient(null), Client.class, searchFilter, sizeLimit);
    }

    public List<Client> getAllClients(int sizeLimit) {
        return persistenceEntryManager.findEntries(getDnForClient(null), Client.class, null, sizeLimit);
    }

    public List<Client> getAllClients() {
        return persistenceEntryManager.findEntries(getDnForClient(null), Client.class, null);
    }


    public Client getClientByDn(String Dn) {
        try {
            return persistenceEntryManager.find(Client.class, Dn);
        } catch (Exception e) {
            logger.warn("", e);
            return null;
        }

    }

    public OxAuthApplicationType[] getApplicationType() {
        return OxAuthApplicationType.values();
    }

    public OxAuthSubjectType[] getSubjectTypes() {
        return OxAuthSubjectType.values();
    }


    public SignatureAlgorithm[] getSignatureAlgorithms() {
        return SignatureAlgorithm.values();
    }

    public SignatureAlgorithm[] getSignatureAlgorithmsWithoutNone() {
        return new SignatureAlgorithm[]{
                SignatureAlgorithm.HS256, SignatureAlgorithm.HS384, SignatureAlgorithm.HS512,
                SignatureAlgorithm.RS256, SignatureAlgorithm.RS384, SignatureAlgorithm.RS512,
                SignatureAlgorithm.ES256, SignatureAlgorithm.ES384, SignatureAlgorithm.ES512,
                SignatureAlgorithm.PS256, SignatureAlgorithm.PS384, SignatureAlgorithm.PS512
        };
    }


    public KeyEncryptionAlgorithm[] getKeyEncryptionAlgorithms() {
        return KeyEncryptionAlgorithm.values();
    }


    public BlockEncryptionAlgorithm[] getBlockEncryptionAlgorithms() {
        return BlockEncryptionAlgorithm.values();
    }

    public AuthenticationMethod[] getAuthenticationMethods() {
        return AuthenticationMethod.values();
    }

    public Client getClientByInum(String inum) {
        Client result = null;
        try {
            result = persistenceEntryManager.find(Client.class, getDnForClient(inum));
        } catch (Exception ex) {
            logger.error("Failed to load client entry", ex);
        }
        return result;
    }

}
