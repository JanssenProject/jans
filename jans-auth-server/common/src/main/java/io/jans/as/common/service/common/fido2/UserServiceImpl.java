package io.jans.as.common.service.common.fido2;

import java.util.List;

import io.jans.as.model.config.StaticConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import io.jans.as.model.configuration.AppConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named("usrService")
public class UserServiceImpl extends io.jans.as.common.service.common.UserService {

	@Inject
	private AppConfiguration appConfiguration;
	@Inject
	private StaticConfiguration staticConfiguration;

	@Override
	public List<String> getPersonCustomObjectClassList() {
		return appConfiguration.getPersonCustomObjectClassList();
	}

	@Override
	public String getPeopleBaseDn() {
		return staticConfiguration.getBaseDn().getPeople();
	}

}