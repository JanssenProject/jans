/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.mapper;


import io.jans.configapi.plugin.keycloak.idp.broker.model.IdentityProvider;
import jakarta.enterprise.context.ApplicationScoped;

import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;


@Mapper
@ApplicationScoped
public interface IdentityProviderMapper {

  IdentityProviderMapper INSTANCE = Mappers.getMapper(IdentityProviderMapper.class);  
  
  @Mapping(target = "inum", source = "kcIdentityProviderRepresentation.internalId")
  @Mapping(target = "name", source = "kcIdentityProviderRepresentation.alias")
  IdentityProvider kcIdentityProviderToIdentityProvider(IdentityProviderRepresentation kcIdentityProviderRepresentation);  

  @Mapping(target = "internalId", source = "identityProvider.inum")
  @Mapping(target = "alias", source = "identityProvider.name")
  IdentityProviderRepresentation identityProviderToKCIdentityProvider(IdentityProvider identityProvider);
}
