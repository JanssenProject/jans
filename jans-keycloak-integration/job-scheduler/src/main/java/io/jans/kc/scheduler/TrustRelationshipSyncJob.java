package io.jans.kc.scheduler;

import io.jans.kc.scheduler.job.ExecutionContext;
import io.jans.kc.scheduler.job.RecurringJob;
import io.jans.saml.metadata.model.EntityDescriptor;
import io.jans.saml.metadata.model.SAMLMetadata;
import io.jans.kc.api.admin.client.KeycloakApi;
import io.jans.kc.api.admin.client.model.ManagedSamlClient;
import io.jans.kc.api.admin.client.model.AuthenticationFlow;
import io.jans.kc.api.config.client.JansConfigApi;
import io.jans.kc.api.config.client.model.JansTrustRelationship;
import io.jans.kc.scheduler.App;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TrustRelationshipSyncJob extends RecurringJob {
    
    private static final Logger log = LoggerFactory.getLogger(TrustRelationshipSyncJob.class);

    private JansConfigApi jansConfigApi;
    private KeycloakApi keycloakApi;
    private String realm;
    private AuthenticationFlow authnBrowserFlow;

    public TrustRelationshipSyncJob() {

        this.jansConfigApi = App.jansConfigApi();
        this.keycloakApi = App.keycloakApi();
        this.realm = App.configuration().keycloakResourcesRealm();
    }
    
    @Override
    public void run(ExecutionContext context) {
        
        try {

            log.debug("Initializing job objects");
            initJob();
            log.debug("Performing Saml client housekeeping");
            performSamlClientsHousekeeping();
            log.debug("Saml client housekeeping complete");

            log.debug("Creating new managed saml clients");
            createNewManagedSamlClients();
            log.debug("Creating new managed saml clients complete");
        }catch(Exception e) {
            log.error("Error running tr sync job",e);
        }
    }

    private void initJob() {
        
        authnBrowserFlow = keycloakApi.getAuthenticationFlowFromAlias(realm,App.configuration().keycloakResourcesBrowserFlowAlias());
    }

    private void performSamlClientsHousekeeping() {

        deleteUnmanagedSamlClients();
    }

    private void deleteUnmanagedSamlClients() {

        log.debug("Deleting unmanaged SAML clients");
        List<ManagedSamlClient> managedsamlclients = keycloakApi.findAllManagedSamlClients(realm);
        managedsamlclients.forEach((c) -> {
           if(!jansConfigApi.trustRelationshipExists(c.externalRef())) {
               log.debug("Deleting previously managed Saml client with id: {}",c.keycloakId());
               keycloakApi.deleteManagedSamlClient(realm,c);
           }
        });
    }

    private void createNewManagedSamlClients() {
        log.debug("Creating new managed Saml clients");
        unassociatedJansTrustRelationships().stream().forEach(this::createNewManagedSamlClient);
    }

    private void createNewManagedSamlClient(final JansTrustRelationship trustrelationship) {
        try {
            SAMLMetadata metadata = jansConfigApi.getTrustRelationshipSamlMetadata(trustrelationship);
            List<EntityDescriptor> entitydescriptors = metadata.getEntityDescriptors();
            if(!entitydescriptors.isEmpty()) {
                //use first entity descriptor 
                keycloakApi.createManagedSamlClient(realm,trustrelationship.getInum(),authnBrowserFlow,entitydescriptors.get(0));
            }
        }catch(Exception e) {
            log.warn("Could not create managed saml client using Trustrelationship with inum " + trustrelationship.getInum(),e);
        }
    }

    private List<JansTrustRelationship> unassociatedJansTrustRelationships() {

        List<JansTrustRelationship> alltr = jansConfigApi.findAllTrustRelationships();
        List<ManagedSamlClient> clients = keycloakApi.findAllManagedSamlClients(realm);
        return alltr.stream().filter((t)-> {
            return clients.stream().noneMatch((c) -> {return c.externalRef().equals(t.getInum());});
        }).toList();
    }

}
