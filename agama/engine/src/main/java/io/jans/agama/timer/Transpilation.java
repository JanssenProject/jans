package io.jans.agama.timer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.agama.dsl.TranspilationResult;
import io.jans.agama.dsl.Transpiler;
import io.jans.agama.dsl.TranspilerException;
import io.jans.agama.dsl.error.SyntaxException;
import io.jans.agama.engine.misc.FlowUtils;
import io.jans.agama.engine.service.AgamaPersistenceService;
import io.jans.agama.model.Flow;
import io.jans.agama.model.Flow.ATTR_NAMES;
import io.jans.agama.model.FlowMetadata;
import io.jans.agama.model.ProtoFlow;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;

@ApplicationScoped
public class Transpilation {
    
    private static final int DELAY = 10 + (int) (10 * Math.random());    //seconds
    private static final int INTERVAL = 30;    // seconds
    private static final double PR = 0.25;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    private Logger logger;
    
    @Inject
    private Event<TimerEvent> timerEvent;
    
    @Inject
    private ObjectMapper mapper;
    
    @Inject
    private FlowUtils futils;

    private AtomicBoolean isActive;
    
    private Map<String, Integer> traces;

    public void initTimer() {
        
        logger.info("Initializing Agama transpilation Timer");
        isActive = new AtomicBoolean(false);
        timerEvent.fire(new TimerEvent(new TimerSchedule(DELAY, INTERVAL),
                new TranspilationEvent(), Scheduled.Literal.INSTANCE));
        
    }
    
    @Asynchronous
    public void run(@Observes @Scheduled TranspilationEvent event) {

        if (!futils.serviceEnabled()) return;

        if (isActive.get()) return;
        
        if (!isActive.compareAndSet(false, true)) return;

        try {
            process();
            logger.debug("Transpilation timer has run.");
        } catch (Exception e) {
            logger.error("An error occurred while running transpilation timer", e);
        } finally {
            isActive.set(false);
        }
        
    }

    /**
     * This method assumes that when a flow is created (eg. via an administrative tool), 
     * attribute revision is set to a negative value
     * @throws IOException 
     */
    public void process() throws IOException {

        List<ProtoFlow> flows = entryManager.findEntries(AgamaPersistenceService.AGAMA_FLOWS_BASE,
                ProtoFlow.class, null);

        Map<String, ProtoFlow> map = flows.stream().collect(
                Collectors.toMap(ProtoFlow::getQname, Function.identity()));

        if (traces == null) {
            traces = map.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey, e -> e.getValue().getRevision()));
            //make it modifiable
            traces = new HashMap<>(traces);
        } else {
            //remove flows that were disabled/removed wrt the previous timer run
            traces.keySet().retainAll(map.keySet());
        }

        List<String> candidates = new ArrayList<>();
        for (String name : map.keySet()) {
            
            ProtoFlow pfl = map.get(name);
            Integer rev = pfl.getRevision();

            if (rev != null) {
                if (!traces.containsKey(name)) {
                    //A newcomer. This script was enabled recently
                    candidates.add(name);
                    traces.put(name, rev);
                } else if (
                    //there might be a compilation of this script running already.
                    //If the node in charge of this crashed before completion, the random 
                    //condition helps to get the job done by another node in the near future
                    (pfl.getTransHash() == null && Math.random() < PR) ||
                    (rev < 0 || rev > traces.get(name))) {
                    candidates.add(name);
                }
            }
        }

        int s = candidates.size();
        if (s > 0) {
            //pick only one. This is helpful in a multinode environment so not all nodes try
            //to work on the same script. However, in practice s will rarelly be greater than 2
            String qname = candidates.get((int)(s * Math.random()));
            logger.info("Starting transpilation of flow '{}'", qname);

            ProtoFlow pfl = map.get(qname);
            pfl.setTransHash(null);  //This helps prevent several nodes transpiling the same flow code
            if (pfl.getRevision() < 0) {
                pfl.setRevision(0);
            }
            
            logger.debug("Marking the script is under compilation");                
            entryManager.merge(pfl);
            traces.put(qname, pfl.getRevision());

            //This time retrieve all attributes for the flow of interest
            Flow fl = entryManager.findEntries(AgamaPersistenceService.AGAMA_FLOWS_BASE,
                Flow.class, Filter.createEqualityFilter(ATTR_NAMES.QNAME, qname), null, 1).get(0);

            String error = null, shortError = null;
            try {
                TranspilationResult result = Transpiler.transpile(qname, fl.getSource());
                logger.debug("Successful transpilation");
                
                FlowMetadata meta = fl.getMetadata();
                meta.setFuncName(result.getFuncName());
                meta.setInputs(result.getInputs());
                meta.setTimeout(result.getTimeout());

                String compiled = result.getCode();
                fl.setMetadata(meta);
                fl.setTranspiled(compiled);
                fl.setTransHash(futils.hash(compiled));
                fl.setCodeError(null);
                
                logger.debug("Persisting changes...");
                entryManager.merge(fl);
                
            } catch (SyntaxException se) {
                try {
                    error = mapper.writeValueAsString(se);
                    shortError = se.getMessage();
                } catch(JsonProcessingException je) {
                    error = je.getMessage();
                }
            } catch (TranspilerException te) {
                error = te.getMessage();
                if (te.getCause() != null) {
                    error += "\n" + te.getCause().getMessage();
                }
            }
            
            if (error != null) {
                
                logger.error("Transpilation failed!");
                if (shortError != null) {
                    logger.error(shortError);
                }
                
                fl.setCodeError(error);
                logger.debug("Persisting error details...");

                entryManager.merge(fl);
                logger.warn("Check database for errors");
            }
        }
        
    }
    
}
