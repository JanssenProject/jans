package io.jans.casa.plugins.sample;

import java.util.Map;

import io.jans.casa.misc.Utils;
import io.jans.casa.service.IPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;

/**
 * A ZK
 * <a href="http://books.zkoss.org/zk-mvvm-book/8.0/viewmodel/index.html" target
 * ="_blank">ViewModel</a> that acts as the "controller" of page
 * <code>index.zul</code> in this sample plugin. See <code>viewModel</code>
 * attribute of panel component of <code>index.zul</code>.
 * 
 * @author jgomer
 */
public class SampleVM {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private String acr = "casa";
	private Map<String, String> map;

	public Map<String, String> getMap() {
		return map;
	}

	public void setMap(Map<String, String> map) {
		this.map = map;
	}

	private IPersistenceService persistenceService;

	@Init
	public void init() {
		logger.info("Sample ViewModel inited");
		persistenceService = Utils.managedBean(IPersistenceService.class);
		map = persistenceService.getCustScriptConfigProperties(acr);
	}

	public String getAcr() {
		return acr;
	}

	public void setAcr(String acr) {
		this.acr = acr;
	}

}
