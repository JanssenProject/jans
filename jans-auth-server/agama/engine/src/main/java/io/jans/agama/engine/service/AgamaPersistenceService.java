package io.jans.agama.engine.service;

import io.jans.agama.engine.misc.FlowUtils;
import io.jans.agama.engine.model.FlowResult;
import io.jans.agama.engine.model.FlowRun;
import io.jans.agama.engine.model.FlowStatus;
import io.jans.agama.engine.model.ProtoFlowRun;
import io.jans.agama.engine.serialize.ContinuationSerializer;
import io.jans.agama.model.Flow;
import io.jans.agama.model.ProtoFlow;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.util.Pair;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.mozilla.javascript.NativeContinuation;
import org.mozilla.javascript.Scriptable;
import org.slf4j.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

@ApplicationScoped
public class AgamaPersistenceService {
        
    private static final String AGAMA_BASE = "ou=agama,o=jans";

    public static final String AGAMA_FLOWRUNS_BASE = "ou=runs," + AGAMA_BASE;
    public static final String AGAMA_FLOWS_BASE = "ou=flows," + AGAMA_BASE;

    @Inject
    private Logger logger;

    @Inject
    private PersistenceEntryManager entryManager;
    
    @Inject
    private ContinuationSerializer contSerializer;
    
    @Inject
    private FlowUtils flowUtils;
    
    @Inject
    private AppConfiguration appConfiguration;
    
    public FlowStatus getFlowStatus(String sessionId) throws IOException {

        try {
            logger.debug("Retrieving current flow's status");
            ProtoFlowRun fr = entryManager.findEntries(AGAMA_FLOWRUNS_BASE, ProtoFlowRun.class, 
                frEqFilter(sessionId), new String[]{ FlowRun.ATTR_NAMES.STATUS }, 1).get(0);

            return fr.getStatus();
        } catch(Exception e) {
            return null;
        }

    }
    
    public void persistFlowStatus(String sessionId, FlowStatus fst) throws IOException {        

        try {
            ProtoFlowRun pfr = entryManager.findEntries(AGAMA_FLOWRUNS_BASE, ProtoFlowRun.class, 
                frEqFilter(sessionId), new String[]{ FlowRun.ATTR_NAMES.ID }, 1).get(0);
            
            logger.debug("Saving current flow's status");
            pfr.setStatus(fst);
            entryManager.merge(pfr);
        } catch(Exception e) {
            throw new IOException(e);
        }

    }
    
    public void createFlowRun(String id, FlowStatus fst, long expireAt) throws Exception {

        FlowRun fr = new FlowRun();
        fr.setBaseDn(String.format("%s=%s,%s", FlowRun.ATTR_NAMES.ID, id, AGAMA_FLOWRUNS_BASE));
        fr.setId(id);
        fr.setStatus(fst);
        fr.setDeletableAt(new Date(expireAt));

        logger.info("Creating flow run");
        entryManager.persist(fr);
        
    }
    
    public boolean flowEnabled(String flowName) {
        
        try {
            Filter filth = Filter.createANDFilter(
                    Filter.createEqualityFilter(Flow.ATTR_NAMES.QNAME, flowName),
                    Filter.createEqualityFilter("jansEnabled", true));
            
            List<ProtoFlow> results = entryManager.findEntries(AGAMA_FLOWS_BASE, 
               ProtoFlow.class, filth, new String[]{ Flow.ATTR_NAMES.QNAME }, 1);
            return results.size() == 1;
             
        } catch(Exception e) {
            logger.error(e.getMessage(), e);
            logger.warn("Flow '{}' does not seem to exist!", flowName);
            return false;
        }
        
    }
    
    public int getEffectiveFlowTimeout(String flowName) {

        Flow fl = entryManager.findEntries(AGAMA_FLOWS_BASE, Flow.class, 
               Filter.createEqualityFilter(Flow.ATTR_NAMES.QNAME, flowName),
               new String[]{ Flow.ATTR_NAMES.META }, 1).get(0);

        int unauth = appConfiguration.getSessionIdUnauthenticatedUnusedLifetime();
        Integer flowTimeout = fl.getMetadata().getTimeout();
        int timeout = Optional.ofNullable(flowTimeout).map(Integer::intValue).orElse(unauth);
        return Math.min(unauth, timeout);

    }
    
    public Flow getFlow(String flowName, boolean full) throws IOException {

        try {
            String[] attrs = null;
            if (!full) {
                attrs = new String[]{ Flow.ATTR_NAMES.QNAME, Flow.ATTR_NAMES.META,
                    Flow.ATTR_NAMES.TRANSPILED };
            }
            
            logger.debug("Retrieving {}info of flow '{}'", full ? "" : "minimal ", flowName);
            List<Flow> fls = entryManager.findEntries(AGAMA_FLOWS_BASE, Flow.class, 
               Filter.createEqualityFilter(Flow.ATTR_NAMES.QNAME, flowName), attrs, 1);
            
            if (fls.isEmpty()) {
                logger.warn("Flow '{}' does not exist!", flowName);
            }
            
            return fls.get(0);
        } catch(Exception e) {
            throw new IOException(e);
        }

    }
    
    public Pair<Scriptable, NativeContinuation> getContinuation(String sessionId)
            throws IOException {
        
        FlowRun fr;
        try {
            fr = entryManager.findEntries(AGAMA_FLOWRUNS_BASE, FlowRun.class, frEqFilter(sessionId),
                    new String[] { "agFlowEncCont", "jansCustomMessage" }, 1).get(0);
        } catch(Exception e) {
            return null;
        }

        logger.debug("Restoring continuation data...");
        byte[] cont = Base64.getDecoder().decode(fr.getEncodedContinuation());
        
        if (!flowUtils.hash(cont).equals(fr.getHash()))
            throw new IOException("Serialized continuation has been altered");

        return contSerializer.restore(cont);
        
    }

    public void saveState(String sessionId, FlowStatus fst, NativeContinuation continuation,
            Scriptable scope) throws IOException {

        byte[] bytes = contSerializer.save(scope, continuation);
        logger.debug("Continuation serialized ({} bytes)", bytes.length);
        
        List<FlowRun> results = entryManager.findEntries(AGAMA_FLOWRUNS_BASE, FlowRun.class, 
                frEqFilter(sessionId), new String[]{ FlowRun.ATTR_NAMES.ID, "exp" }, 1);
        //The query above retrieves enough attributes so no data is lost after the
        //update that follows below
        
        FlowRun run = results.get(0);
        run.setEncodedContinuation(new String(Base64.getEncoder().encode(bytes), UTF_8));
        run.setHash(flowUtils.hash(bytes));
        //overwrite status
        run.setStatus(fst);
        
        logger.debug("Saving state of current flow run");
        entryManager.merge(run);
        
    }
    
    public void finishFlow(String sessionId, FlowResult result) throws IOException {
        
        try {
            logger.debug("Retrieving flow run {}", sessionId);
            FlowRun run = entryManager.findEntries(AGAMA_FLOWRUNS_BASE, FlowRun.class, frEqFilter(sessionId),
                    new String[]{ FlowRun.ATTR_NAMES.ID, FlowRun.ATTR_NAMES.STATUS, "exp" }, 1).get(0);

            //The query above retrieves enough attributes so no data is lost after the
            //update that follows below

            FlowStatus status = run.getStatus();
            status.setStartedAt(FlowStatus.FINISHED);
            status.setResult(result);

            status.setQname(null);
            status.setJsonInput(null);
            status.setParentsMappings(null);                
            status.setTemplatePath(null);
            status.setTemplateDataModel(null);
            status.setExternalRedirectUrl(null);

            run.setEncodedContinuation(null);
            run.setHash(null);

            logger.info("Marking flow run as finished...");
            entryManager.merge(run);
            
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void terminateFlow(String sessionId) throws IOException {
    
        try {
            logger.info("Removing flow run...");
            entryManager.remove(AGAMA_FLOWRUNS_BASE, FlowRun.class, frEqFilter(sessionId), 1);
        } catch (Exception e) {
            throw new IOException(e);
        }
        
    }
    
    private Filter frEqFilter(String id) {
        return Filter.createEqualityFilter(FlowRun.ATTR_NAMES.ID, id);
    }

}
