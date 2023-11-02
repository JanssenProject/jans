/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.mapper;


import io.jans.configapi.plugin.keycloak.idp.broker.model.IdentityProvider;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.exception.InvalidAttributeException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;

@Mapper
public interface IdentityProviderMapper {

  IdentityProviderMapper INSTANCE = Mappers.getMapper(IdentityProviderMapper.class);  
   
  IdentityProvider kcIdentityProviderToIdentityProvider(IdentityProviderRepresentation kcIdentityProviderRepresentation);  

  IdentityProviderRepresentation identityProviderToKCIdentityProvider(IdentityProvider identityProvider);
}
