package io.jans.casa.plugins.bioid.vm;

import io.jans.casa.misc.Utils;
import io.jans.casa.misc.WebUtils;
import io.jans.casa.plugins.bioid.BioIdService;
import io.jans.casa.service.IPersistenceService;
import io.jans.casa.service.ISessionContext;
import io.jans.util.Pair;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.QueryParam;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventQueues;
import org.zkoss.zk.ui.select.annotation.WireVariable;

public class RedirectViewModel {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private String text;
	private String title;
	private String serverUrl;
	private BioIdService bis;

	public RedirectViewModel() {
		bis = BioIdService.getInstance();
		serverUrl = Utils.managedBean(IPersistenceService.class).getIssuerUrl();
	}

	public String getText() {
		return text;
	}

	public String getTitle() {
		return title;
	}

	@Init
	public void init(@QueryParam("start") String start) {
		try {
			String currentUrl = WebUtils.getServletRequest().getRequestURL().toString();
			title = Labels.getLabel("general.error.general");
			if (Utils.isNotEmpty(start)) {
				Map<String, String> creds = bis.getCasaClient();
				String agamaUrl = makeOauthRequestUrl(creds, currentUrl);
				WebUtils.execRedirect(agamaUrl, false);
			} else {
				title = "Some title";
				text = "Some text";
			}

		} catch (Exception e) {
			text = e.getMessage();
			logger.error(text, e);
		}
	}

	private String makeOauthRequestUrl(Map<String, String> client, String currentUrl) {
		StringBuilder s = new StringBuilder();
		s.append(serverUrl);
		s.append("/jans-auth/restv1/authorize");
		s.append("?acr_values=agama&agama_flow=io.jans.agama.bioid.enroll");
		s.append("&response_type=code");
		s.append("&state=abcdef");
		s.append("&client_id=" + client.get("client_id"));
		s.append("&redirect_uri=" + currentUrl);
		s.append("&prompt=login");
		return s.toString();
	}

}
