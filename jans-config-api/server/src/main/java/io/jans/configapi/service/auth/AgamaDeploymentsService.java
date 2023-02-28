package io.jans.configapi.service.auth;

import io.jans.ads.model.Deployment;
import io.jans.ads.model.DeploymentDetails;
import io.jans.orm.model.PagedResult;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Base64;
import java.util.Date;

import org.slf4j.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

@ApplicationScoped
public class AgamaDeploymentsService {

    private static final String BASE_DN = "ou=deployments,ou=agama,o=jans";
    
    @Inject
    private Logger logger;

    @Inject
    private PersistenceEntryManager entryManager;

    public PagedResult<Deployment> list(int start, int count, int maxCount) {
        
        String[] attrs = new String[]{ "jansId", "jansStartDate", "jansActive",
                "jansEndDate", "adsPrjDeplDetails" };
        Filter filter = Filter.createPresenceFilter("jansId");

        return entryManager.findPagedEntries(BASE_DN, Deployment.class,
                filter, attrs, "jansStartDate", null, start, count, maxCount);

    }

    public Deployment getDeployment(String name) {
        
        String[] attrs = new String[]{ "jansStartDate", "jansEndDate", "adsPrjDeplDetails" };
        logger.info("Looking up project named {}", name);

        Deployment d = null;
        try {
            d = entryManager.find(dnOfProject(idFromName(name)), Deployment.class, attrs);
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }
        return d;

    }
    
    public boolean createDeploymentTask(String name, byte[] gamaBinary) {
        
        Deployment d = null;
        String id = idFromName(name);
        try {
            String[] attrs = new String[]{ "jansActive", "jansEndDate", "dn" };
            d = entryManager.find(dnOfProject(id), Deployment.class, attrs);
        } catch (Exception e) {
            logger.debug("No already existing deployment for project {}", name);
        }
        
        boolean existing = d != null;
        if (existing && d.getFinishedAt() == null) {
            logger.info("A deployment is still in course for this project!");
            
            if (!d.isTaskActive()) {
                logger.info("No node is in charge of this task yet");
            }
            return false;
        }
        
        DeploymentDetails dd = new DeploymentDetails();
        dd.getProjectMetadata().setProjectName(name);

        if (!existing) {
            d = new Deployment();        
            d.setDn(dnOfProject(id));
        }
        d.setId(id);
        d.setCreatedAt(new Date());
        d.setTaskActive(false);
        d.setFinishedAt(null);
        d.setDetails(dd);
        
        byte[] encoded = Base64.getEncoder().encode(gamaBinary);
        d.setAssets(new String(encoded, UTF_8));
        
        logger.info("Persisting deployment task for project {}", name);
        if (existing) {
            entryManager.merge(d);
        } else {
            entryManager.persist(d);
        }
        return true;
        
    }
    
    public Boolean createUndeploymentTask(String name) {
        
        String dn = dnOfProject(idFromName(name));        
        String[] attrs = new String[]{ "jansActive", "dn" };
        Deployment d = null;
        
        try {
            d = entryManager.find(dn, Deployment.class, attrs);
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }

        if (d == null) return null;
        
        //A project can be undeployed when there is no deployment in course or
        //when there is but no node has taken it yet
        boolean deploymentInProcess = d.isTaskActive();
        if (!deploymentInProcess) {
            logger.info("Removing deployment of project {}", name);
            entryManager.remove(dn, Deployment.class);
        }

        return !deploymentInProcess;
        
    }

    private static String dnOfProject(String prjId) {
        return String.format("jansId=%s,%s", prjId, BASE_DN);
    }

    private static String idFromName(String name) {
        String hash = Integer.toString(name.hashCode());
        if (hash.startsWith("-")) hash = hash.substring(1);
        return hash;
    }

}