package io.jans.casa.plugins.bioid.vm;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.zk.ui.select.annotation.WireVariable;

import io.jans.casa.core.pojo.User;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.bioid.BioIdService;
import io.jans.casa.service.ISessionContext;
import io.jans.casa.service.SndFactorAuthenticationUtils;

public class BioIdViewModel {
	private Logger logger = LoggerFactory.getLogger(getClass());
	@WireVariable
	private ISessionContext sessionContext;
	private SndFactorAuthenticationUtils sndFactorUtils;
	private User user;
	private BioIdService bis;

	/**
	 * Initialization method for this ViewModel.
	 */
	@Init
	public void init() {
		logger.debug("BioID: Init invoked");
		sessionContext = Utils.managedBean(ISessionContext.class);
		user = sessionContext.getLoggedUser();
		sndFactorUtils = Utils.managedBean(SndFactorAuthenticationUtils.class);
		bis = BioIdService.getInstance();
	}
}
