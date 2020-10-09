package org.gluu.oxtrust.service.radius;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.oxtrust.service.OrganizationService;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.radius.model.RadiusClient;
import org.gluu.search.filter.Filter;
import org.slf4j.Logger;

@ApplicationScoped
public class GluuRadiusClientService  implements Serializable{

    /**
	 * 
	 */
    private static final long serialVersionUID = 8095893988896942051L;
    
    private static final String CLIENT_NAME_ATTR = "oxRadiusClientName";
	private static final String CLIENT_IP_ADDRESS_ATTR = "oxRadiusClientIpAddress";

	@Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private Logger logger;

    public List<RadiusClient> getAllClients() {

        String clientsBaseDn = getRadiusClientsBaseDn();
        return persistenceEntryManager.findEntries(clientsBaseDn,RadiusClient.class,null);
    }

    public List<RadiusClient> getAllClients(Integer sizeLimit) {

        String clientsBaseDn = getRadiusClientsBaseDn();
        return persistenceEntryManager.findEntries(clientsBaseDn,RadiusClient.class,null,sizeLimit);
    }

    public List<RadiusClient> searchClients(String pattern, Integer sizeLimit) {

        String [] targetArray = new String[] {pattern};
        String clientsBaseDn = getRadiusClientsBaseDn();
        Filter nameFilter = Filter.createSubstringFilter(CLIENT_NAME_ATTR,null,targetArray,null);
        Filter ipFilter = Filter.createSubstringFilter(CLIENT_IP_ADDRESS_ATTR,null,targetArray,null);
        Filter searchFilter=  Filter.createORFilter(nameFilter,ipFilter);
        return persistenceEntryManager.findEntries(clientsBaseDn,RadiusClient.class,searchFilter,sizeLimit);
    }

    public RadiusClient getRadiusClientByInum(String inum) {

        RadiusClient client = null;
        try {
            client = persistenceEntryManager.find(RadiusClient.class,getRadiusClientDn(inum));
        }catch(Exception e) {
            logger.debug("Could not load radius client",e);
        }
        return client;
    }

    public void addRadiusClient(RadiusClient client) {
        persistenceEntryManager.persist(client);
    }

    public void updateRadiusClient(RadiusClient client)  {
        persistenceEntryManager.merge(client);
    }

    public void deleteRadiusClient(RadiusClient client) {
        persistenceEntryManager.remove(client);
    }

    public String getRadiusClientsBaseDn() {
        return String.format("ou=radius_clients,%s",organizationService.getDnForOrganization());
    }

    public String getRadiusClientDn(String inum) {
    
        if(inum == null)
            return null;
        String orgDn = organizationService.getDnForOrganization();
        return String.format("inum=%s,ou=radius_clients,%s",inum,orgDn);
    }

    public String generateInum() {

        String inum = null;
        String dn = null;
        do {
            inum = generateInumInternal();
            dn = getRadiusClientDn(inum);
        }while(persistenceEntryManager.contains(dn,RadiusClient.class));
        return inum;
    }

    public String generateInumInternal() {

        return UUID.randomUUID().toString();
    }
}