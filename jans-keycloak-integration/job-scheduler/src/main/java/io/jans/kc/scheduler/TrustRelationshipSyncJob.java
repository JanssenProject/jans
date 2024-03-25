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
import java.util.Optional;
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
        try {
            this.authnBrowserFlow = keycloakApi.getAuthenticationFlowFromAlias(realm,App.configuration().keycloakResourcesBrowserFlowAlias());
        }catch(Exception e) {
            log.warn("Could not properly initialize sync job",e);
        }
    }
    
    @Override
    public void run(ExecutionContext context) {
        
        try {

            
            log.debug("Performing Saml client housekeeping");
            performSamlClientsHousekeeping();
            log.debug("Saml client housekeeping complete");

            log.debug("Creating new managed saml clients");
            createNewManagedSamlClients();
            log.debug("Creating new managed saml clients complete");
            
            log.debug("Updating existing managed saml clients");
            updateExistingManagedSamlClients();
            log.debug("Updating existing managed saml clients complete");
        }catch(Exception e) {
            log.error("Error running tr sync job",e);
        }
    }

    private void performSamlClientsHousekeeping() {

        deleteUnmanagedSamlClients();
    }

    private void deleteUnmanagedSamlClients() {

        log.debug("Deleting unmanaged SAML clients");
        List<ManagedSamlClient> managedsamlclients = keycloakApi.findAllManagedSamlClients(realm);
        if(managedsamlclients.isEmpty()) {
            log.debug("No previously managed SAML clients found in keycloak.");
            return;
        }else {
            log.debug("Previously managed SAML clients found in keycloak. Count: {}",managedsamlclients.size());
        }

        managedsamlclients.forEach((c) -> {
           if(!jansConfigApi.trustRelationshipExists(c.externalRef())) {
               log.debug("Deleting previously managed SAML client with id: {}",c.keycloakId());
               keycloakApi.deleteManagedSamlClient(realm,c);
           }
        });
    }

    private void createNewManagedSamlClients() {
        
        List<JansTrustRelationship> unassociatedtrs = unassociatedJansTrustRelationships();
        if(unassociatedtrs.isEmpty()) {
            log.debug("No unmanaged trust relationships found in Janssen.");
            return;
        }else {
            log.debug("Unmanaged trust relationships found in Janssen. Count: {}",unassociatedtrs.size());
        }
        unassociatedtrs.stream().forEach(this::createNewManagedSamlClient);
    }

    private void createNewManagedSamlClient(final JansTrustRelationship trustrelationship) {
        try {
            log.debug("Creating managed SAML client from Janssen TR with inum {}",trustrelationship.getInum());
            SAMLMetadata metadata = jansConfigApi.getTrustRelationshipSamlMetadata(trustrelationship);
            List<EntityDescriptor> entitydescriptors = metadata.getEntityDescriptors();
            if(!entitydescriptors.isEmpty()) {
                //use first entity descriptor 
                String trinum = trustrelationship.getInum();
                ManagedSamlClient client = keycloakApi.createManagedSamlClient(realm,trinum,authnBrowserFlow,entitydescriptors.get(0));
                log.debug("Created managed SAML client with id {} from Janssen TR with inum {}",client.keycloakId(),trinum);
            }
        }catch(Exception e) {
            log.warn("Could not create managed SAML client using tr with inum {}",trustrelationship.getInum());
            log.warn("Resulting exception: ",e);
        }
    }

    private void updateExistingManagedSamlClients() {

        List<JansTrustRelationship> alltr = jansConfigApi.findAllTrustRelationships();
        List<ManagedSamlClient> clients = keycloakApi.findAllManagedSamlClients(realm);
        log.debug("Updating existing managed saml clients. Count: {}",clients.size());
        clients.stream().forEach((c) -> {
            Optional<JansTrustRelationship> tr  = alltr
                .stream()
                .filter((t)->{return c.correspondsToExternalRef(t.getInum());})
                .findFirst();
            if(tr.isPresent()) {
                this.updateExistingSamlClient(c,tr.get());
            }
        });
    }

    private void updateExistingSamlClient(ManagedSamlClient client, JansTrustRelationship trustrelationship) {

        try {
            log.debug("Updating managed SAML client with id {}. Associated trust relationship inum: {}",client.keycloakId(),client.externalRef());
            SAMLMetadata metadata = jansConfigApi.getTrustRelationshipSamlMetadata(trustrelationship);
            List<EntityDescriptor> entitydescriptors = metadata.getEntityDescriptors();
            if(!entitydescriptors.isEmpty()) {
                keycloakApi.updateManagedSamlClient(realm, client, entitydescriptors.get(0));
            }
        }catch(Exception e) {
            log.warn("Could not update managed SAML client with id {}",client.keycloakId());
            log.warn("Resulting exception: ",e);
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
