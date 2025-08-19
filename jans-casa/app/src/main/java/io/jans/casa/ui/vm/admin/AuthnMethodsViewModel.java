package io.jans.casa.ui.vm.admin;

import io.jans.casa.core.ConfigurationHandler;
import io.jans.casa.core.ExtensionsManager;
import io.jans.casa.core.UserService;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.misc.Utils;
import io.jans.casa.ui.model.AuthnMethodStatus;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import org.pf4j.DefaultPluginDescriptor;
import org.pf4j.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.*;
import org.zkoss.util.Pair;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.annotation.*;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zul.Messagebox;

public class AuthnMethodsViewModel extends MainViewModel {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable("extensionsManager")
    private ExtensionsManager extManager;
    
    private List<AuthnMethodStatus> methods;
    
    private AuthnMethodStatus currentAms;
    
    public List<AuthnMethodStatus> getMethods() {
        return methods;
    }
    
    public AuthnMethodStatus getCurrentAms() {
        return currentAms;
    }

    @Init(superclass = true)
    public void childInit() {

        Map<String, String> mappedAcrs = getSettings().getAcrPluginMap();
        List<Pair<AuthnMethod, PluginDescriptor>> pairs = extManager.getAuthnMethodExts();
        methods = new ArrayList<>();
        
        for (Pair<AuthnMethod, PluginDescriptor> p : pairs) {
            AuthnMethod aMethod = p.getX();
            PluginDescriptor pd = p.getY();
            
            String plug = pd.getPluginId();
            boolean isSystem = plug == null;
            String acr = aMethod.getAcr();
            
            AuthnMethodStatus ams = new AuthnMethodStatus();
            
            if (mappedAcrs.containsKey(acr)) {
                String mappedPlugin = mappedAcrs.get(acr);
                ams.setEnabled(mappedPlugin == null && isSystem || mappedPlugin.equals(plug));
            }            
            
            ams.setAcr(acr);
            ams.setName(Labels.getLabel(aMethod.getUINameKey()));
            ams.setSelectedPlugin(plug);

            plug = isSystem ? Labels.getLabel("adm.method_sysextension") :
                    Labels.getLabel("adm.method_plugin_template", new String[]{ plug, pd.getVersion() });

            ams.setDescription(plug);
            ams.setClassName(aMethod.getClass().getName());
            methods.add(ams);
        }
        
        List<String> pr = getSettings().getAcrPriority();
        
        if (Utils.isNotEmpty(pr)) {
            logger.debug("acr priority list is {}", pr);
            
            //sort as per reverse priority
            methods.sort((ams1, ams2) -> {
                int i = pr.indexOf(ams1.getAcr());
                int j = pr.indexOf(ams2.getAcr());
                
                if (i == -1) {
                    return j == -1 ? 0 : 1;    //for simplicity, both unknown are considered "equal"
                } else if (j == -1) return -1;
    
                //if both are known, it wins the one appearing first
                return i < j ? -1 : 1;
            });
        }
       
    }

	@AfterCompose
	public void afterCompose(@ContextParam(ContextType.VIEW) Component view) {
		Selectors.wireEventListeners(view, this);
	}
	
	@Listen("onData=#meme")
	public void save(Event event) {
	    
	    if (!checkUniqueAcrs()) return;
	    
	    List<Integer> ordering = new ArrayList<>();
        Object[] numbers = (Object[]) event.getData();
        Stream.of(numbers).map(Object::toString).map(Integer::valueOf).forEach(ordering::add);
		
		List<String> pr = new ArrayList<>();
		ordering.forEach(i -> pr.add(methods.get(i).getAcr()));
		logger.info("New acr priority will be {}", pr);
		
		getSettings().setAcrPriority(pr);		
		
		Map<String, String> mapping = new LinkedHashMap<>();    //insertion order is relevant
		for (String acr : pr) {
		    for (AuthnMethodStatus ams : methods) {
		         if (ams.isEnabled() && ams.getAcr().equals(acr)) {
		             mapping.put(acr, ams.getSelectedPlugin());
		             break;
		         }
		    }
		}
		
		logger.info("New plugin mapping will be: {}", mapping);
		getSettings().setAcrPluginMap(mapping);

        updateMainSettings(Labels.getLabel("adm.methods_action"));

	}

    @NotifyChange({ "currentAms" })
    public void showDialog(AuthnMethodStatus ams) {
        currentAms = ams;
    }

    @NotifyChange({ "currentAms" })
	public void closeDialog(Event event) {	    
	    currentAms = null;
	    if (event != null && event.getName().equals(Events.ON_CLOSE)) {
			event.stopPropagation();
		}
	}
	
	private boolean checkUniqueAcrs() {
	    
	    Set<String> acrs = new HashSet<>();
	    String acr = null;
	    
	    for (AuthnMethodStatus ams : methods) {
	        if (ams.isEnabled() && !acrs.add(ams.getAcr())) {
	            acr = ams.getAcr();
	            break;
	        }
	    }
	    if (acr == null) return true;
	    
	    Messagebox.show(Labels.getLabel("adm.methods_acr_conflict", new String[]{ acr }),
	                null, Messagebox.OK, Messagebox.EXCLAMATION);
	    return false;
	    
	}
	
}
